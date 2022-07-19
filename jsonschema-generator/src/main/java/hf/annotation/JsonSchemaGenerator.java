package hf.annotation;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;

import javax.swing.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class JsonSchemaGenerator {

    // 该类是线程安全的 所以在这里直接被定义
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // 接口 可以用于对josn shema检查 很多属性
    private SchemaGeneratorConfig config;
    private Map<String, JsonNode> uiSchemaOptionMap;


    /**
     * 属性的getter和setter方法
     * @return
     */
    public SchemaGeneratorConfig getConfig(){return this.config;}
    public void setConfig(SchemaGeneratorConfig config) {
        this.config = config;
    }
    public void setUiSchemaOptionMap(Map<String, JsonNode> uiSchemaOptionMap) {
        this.uiSchemaOptionMap = uiSchemaOptionMap;
    }

    public Map<String, JsonNode> getUiSchemaOptionMap() {
        return uiSchemaOptionMap;
    }

    /**
     * 将类转换成为一个jsonNode 里面包括着json Schema 和 ui Schema
     * @param className
     * @return
     */
    public JsonNode generate(Class className){
        // 接着反射 生成json schema 的结构
        // https://www.javadoc.io/static/com.github.victools/jsonschema-generator/4.25.0/com/github/victools/jsonschema/generator/SchemaGenerator.html
        SchemaGenerator generator = new SchemaGenerator(this.config);

        // JsonNode 和 ObjectNode的区别在于 JsonNode 不可变 只用于读取 ObejectNode 的可以修改该Json树
        // type 用来描述类的全限定名称的一个反射类
        JsonNode jsonSchema = generator.generateSchema(className, new Type[0]);
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        ObjectNode uiSchema = OBJECT_MAPPER.createObjectNode();

        // 从class的结构定义中 转换为uiSchema
        extractPropertyStructure(jsonSchema, uiSchema, uiSchemaOptionMap);
        root.set("Schema", jsonSchema);
        root.set("uiSchema", uiSchema);
        return root;
    }

    private void extractPropertyStructure(JsonNode node, ObjectNode newNode, Map<String, JsonNode> optionsNodeMap){
        // properties 属于object中的子属性
        JsonNode properties = node.get("properties");
        // forEachRemaining 只会对得到的迭代器剩余的部分进行迭代
        properties.fields().forEachRemaining(filed -> {

            ObjectNode newSubNode = OBJECT_MAPPER.createObjectNode();
            // 获取json对象中的每一项数据 然后获取实际的数值
            // todo map代表的是什么
            Optional.ofNullable(optionsNodeMap.get(filed.getKey())).ifPresent(presentNode -> {

                // uiOption用来修饰 一个对象的一些属性的一个json节点
                newSubNode.set("ui:options", presentNode);
            });

            newNode.set(filed.getKey(), newSubNode);
            // properties下的子节点
            JsonNode subNode = filed.getValue();

            // 该属性下有嵌套的子节点
            if (subNode.isObject() && subNode.has("properties")) {
                extractPropertyStructure(subNode, newSubNode, optionsNodeMap);
            }
        });
    }

    public MoyeJsonSchemaGeneratorBuilder  builder(){

    }

    class MoyeJsonSchemaGeneratorBuilder{
        private MoyeJsonSchemaGeneratorProperty property = new MoyeJsonSchemaGeneratorProperty();
        protected MoyeJsonSchemaGeneratorBuilder(){}

        public MoyeJsonSchemaGeneratorBuilder setAutoJsonSchemaFormat(boolean flag){
            this.property.setAutoJsonSchemaFormat(flag);
            return this;
        }

        public JsonSchemaGenerator build() {
            JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator();
            // 关于  victools中的option https://victools.github.io/jsonschema-generator/#generator-options
            List<Option> optionList = new ArrayList<>();
            Collections.addAll(optionList, new Option[]{
                    // java 中的static final属性会被转换成为 静态属性
                    Option.VALUES_FROM_CONSTANT_FIELDS,
                    // 包含static fileds
                    Option.PUBLIC_STATIC_FIELDS,
                    Option.PUBLIC_NONSTATIC_FIELDS,
                    Option.NONPUBLIC_STATIC_FIELDS,
                    Option.NONPUBLIC_NONSTATIC_FIELDS_WITH_GETTERS,
                    Option.NONPUBLIC_NONSTATIC_FIELDS_WITHOUT_GETTERS,
                    Option.TRANSIENT_FIELDS,
                    Option.SIMPLIFIED_ENUMS,
                    Option.SIMPLIFIED_OPTIONALS,
                    Option.DEFINITIONS_FOR_ALL_OBJECTS,
                    Option.NULLABLE_METHOD_RETURN_VALUES_BY_DEFAULT,
                    Option.ALLOF_CLEANUP_AT_THE_END,
                    Option.FLATTENED_ENUMS_FROM_TOSTRING,
                    Option.INLINE_ALL_SCHEMAS,
                    Option.ADDITIONAL_FIXED_TYPES});

            if (this.property.isAutoJsonSchemaFormat()){
                // 是否启用format
                optionList.add(Option.EXTRA_OPEN_API_FORMAT_VALUES);
            }

            jsonSchemaGenerator.setUiSchemaOptionMap(new HashMap<>());

            // 配置Json Schema version和
            SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12,
                    // toArray(new option[0])代表着直接转换为该属性的数组
                    new OptionPreset(optionList.toArray(new Option[0]))).
                    // 如果使用了@JsonPropertyOrder 会让对象中的属性进行排序
                    with(new JacksonModule(new JacksonOption[]{JacksonOption.RESPECT_JSONPROPERTY_ORDER}));


            // forFileds 可以对所有的类中的属性进行获取 然后使用一些方法可以进行解析
            configBuilder.forFields().
                    // withTargetTypeOverrideResolver : 允许使用传入参数的子类型用来替代这个本身
                    withTargetTypeOverridesResolver(filed -> {
                        // 收集被JsonSchemaProperty注解的属性
                        return Optional.ofNullable(filed.getAnnotation(JsonSchemaProperty.class)).
                                // 处理anyof属性
                                map(JsonSchemaProperty::anyOf).
                                map(Stream::of).
                                // anyof 中的字符串 和实际属性转换
                                map(stream -> {
                                    return stream.map(childType -> {
                                        return  filed.getContext().resolve(childType, new Type[0]);
                                    });
                                }).
                                map(stream -> {
                                    return stream.collect(Collectors.toList());
                                }).


                                orElse(null);
                    }).
                    // 处理标题
                    withTitleResolver(filed -> {
                        return Optional.ofNullable(filed.getAnnotation(JsonSchemaProperty.class)).map(JsonSchemaProperty::title).orElse(null);
                    }).
                    // 处理description
                    withDescriptionResolver(filed -> {
                        return Optional.ofNullable(filed.getAnnotation(JsonSchemaProperty.class)).map(JsonSchemaProperty::description).orElse(null);
                    }).
                    // withInstanceAttributeOverride用来修改json节点中的属性数值
                    // 构造方法 构造出来的类是用来删除或者添加某些json node上的内容的
                    // https://www.javadoc.io/static/com.github.victools/jsonschema-generator/4.24.2/com/github/victools/jsonschema/generator/InstanceAttributeOverrideV2.html
                    withInstanceAttributeOverride(((collectedMemberAttributes, member, context) -> {
                        JsonSchemaUiProperty annotation = member.getAnnotation(JsonSchemaUiProperty.class);
                        Optional.ofNullable(annotation).ifPresent(ans -> {
                            String s = JSON.toJSONString(annotation);
                            try {
                                jsonSchemaGenerator.getUiSchemaOptionMap().put(member.getName(), JsonSchemaGenerator.OBJECT_MAPPER.readTree(s));
                            } catch (JsonProcessingException e) {
                                System.out.println("uiSchema无法转换成为JsonNode");;
                            }
                        });
                    }));
            configBuilder.with(new JakartaValidationModule(new JakartaValidationOption[]{JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS,
                    //
                    JakartaValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED}));

            SchemaGeneratorConfig build = configBuilder.build();
            jsonSchemaGenerator.setConfig(build);
            return jsonSchemaGenerator;
        }


    }


}
