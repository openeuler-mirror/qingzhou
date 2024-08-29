package qingzhou.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 模型的元数据注解，用于定义一个模型，提供模型的基本信息和配置选项，
 * 这些信息主要用于UI展示、菜单构建以及模型的初始化等场景。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Model {
    String code(); // 获取模型的基础名称

    String icon() default ""; // 获取模型在UI中显示的图标名称。

    String menu() default ""; // 模型所属的菜单名

    int order() default 0; // 模型在菜单中的排序顺序，数值越小越靠前

    String entrance() default "list"; // 模型的入口操作，即在菜单上的链接，默认为列表

    boolean hidden() default false; // 是否在页面菜单中隐藏，隐藏后其 REST 等接口依然有效。非人机交换的接口一般需要此设置。

    String[] name(); // 模型名称的国际化数组

    String[] info(); // 模型描述信息的国际化数组
}