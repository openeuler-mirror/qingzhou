package qingzhou.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定义模型的操作。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModelAction {
    // 提供一个国际化资源键数组，用于根据不同的语言环境加载相应的操作名称。
    String[] name();

    // 提供一个国际化资源键数组，用于根据不同的语言环境加载相应的操作详细信息。
    String[] info() default {};

    String code(); // 接口名

    String icon() default ""; // 指定与该模型操作关联的图标名称

    String show() default ""; // 设置该操作的可用条件

    boolean distribute() default false;

    String redirect() default ""; // 转给默认的view，如 qingzhou.api.type.List.ACTION_LIST，注：优先级低于 page

    String page() default ""; // 跳转到指定的页面，注：app根目录后的相对路径

    String[] link_fields() default {}; // 在list页面上，弹出表单页面，指定表单页面里显示的字段

    String[] link_models() default {}; //  弹出子级管理页面
}
