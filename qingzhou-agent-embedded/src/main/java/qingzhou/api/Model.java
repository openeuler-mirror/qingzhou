package qingzhou.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Model {
    String code();
    String icon() default "";
    String menu() default "";
    String action() default "";
    int order() default 0;
    String[] name();
    String[] info();
}