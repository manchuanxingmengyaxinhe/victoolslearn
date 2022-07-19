package hf.annotation;

public class MoyeJsonSchemaGeneratorProperty {
    private boolean autoJsonSchemaFormat = false;

    public MoyeJsonSchemaGeneratorProperty(){}

    public void setAutoJsonSchemaFormat(boolean autoJsonSchemaFormat){
        this.autoJsonSchemaFormat = autoJsonSchemaFormat;
    }
    public boolean isAutoJsonSchemaFormat() {
        return autoJsonSchemaFormat;
    }
}
