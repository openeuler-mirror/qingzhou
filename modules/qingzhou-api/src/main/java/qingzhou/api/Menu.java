package qingzhou.api;

import java.lang.annotation.*;

@Repeatable(Menus.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Menu {
    String[] name();

    String code();

    String icon() default "";

    int order() default 0;

    String parent() default ""; // parent code
}