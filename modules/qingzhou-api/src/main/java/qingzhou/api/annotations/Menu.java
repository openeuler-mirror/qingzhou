package qingzhou.api.annotations;

import java.lang.annotation.*;

/**
 * 声明一个菜单节点，模型通过 {@code @Model.menu} 按 code 挂载其上。可重复标注，由 {@code parent} 组装成多级菜单树。
 * <p>
 * 未声明任何菜单时模型自身作为根节点呈现；某菜单下只有一个模型时，点击菜单直达该模型页面。
 */
@Repeatable(Menus.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Menu {
    /**
     * 菜单名称，国际化数组。
     */
    String[] name();

    /**
     * 菜单标识，被 {@code @Model.menu} 与子菜单的 {@code parent} 引用。
     */
    String code();

    /**
     * 界面图标的名称或素材标识。
     */
    String icon() default "";

    /**
     * 同级排序，越小越靠前。
     */
    int order() default 9;

    /**
     * 父菜单 code，为空表示根节点；引用的父菜单不存在时退化为根节点。
     */
    String parent() default "";
}
