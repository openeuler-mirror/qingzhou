package qingzhou.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@Repeatable(Menus.class)
public @interface Menu {
    String code();
    String[] name();
    String icon() default "";
    int order() default 0;
    String parent() default "";
}