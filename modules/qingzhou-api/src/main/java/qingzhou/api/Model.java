package qingzhou.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 模块的元数据注解，用于定义一个模块，提供模块的基本信息和配置选项，
 * 这些信息主要用于UI展示、菜单构建以及模块的初始化等场景。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Model {
    String code() default "";

    String icon() default "";

    String menu() default "";

    int order() default 9;

    String[] name();

    String[] info() default {};
}