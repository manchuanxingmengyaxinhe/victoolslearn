package hf.annotation;




import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


// type指的是可以作用在类和接口上
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonSchemaProperty {
    // 整个jsonschema的标题
    String title() default "";
    // 解释
    String description() default "";
    // 对于该数据项的例子
    String[] examples() default {};

    // 表示 新加入的条件必须要满足该组合里面的所有数值
    Class<?> [] anyOf() default {};
}
