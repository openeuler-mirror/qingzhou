package qingzhou.api;

import java.lang.annotation.*;

@Repeatable(I18ns.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface I18n {
    String code();

    String[] name();
}