package qingzhou.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标识轻舟应用的启动入口类，整个应用内最多允许一个存在
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface App {
    String code() default "";

    String icon() default "";

    String[] name();

    String[] info() default {};
}
