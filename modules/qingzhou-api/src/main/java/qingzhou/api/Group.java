package qingzhou.api;

import java.lang.annotation.*;

@Repeatable(Groups.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Group {
    String[] name();

    String code();
}