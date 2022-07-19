package hf.annotation;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface JsonSchemaUiProperty {
    String widget() default "";
    byte width() default 100;
    boolean titleShow() default false;
    boolean descriptionShow() default false;
    Location titleLocation() default Location.topLeft;

    Location descriptionLocation() default Location.hover;

    public static enum Location {
        topLeft,
        topRight,
        left,
        bottom,
        hover;

        private Location() {
        }
    }



}
