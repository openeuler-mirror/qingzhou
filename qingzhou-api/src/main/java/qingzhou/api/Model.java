package qingzhou.api;

import qingzhou.api.type.Listable;

import java.lang.annotation.*;

/**
 * 模型的元数据注解。
 * 该注解用于标注在类上，提供模型的基本信息和配置选项，
 * 这些信息主要用于UI展示、菜单构建以及模型的初始化等场景。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Model {

    /**
     * 获取模型的基础名称。
     * @return 模型名称字符串
     */
    String name();

    /**
     * 获取模型在UI中显示的图标名称。
     * @return 图标名称字符串
     */
    String icon();

    /**
     * 获取模型名称的国际化数组，支持多语言版本。
     * @return 包含多个语言版本名称的字符串数组
     */
    String[] nameI18n();

    /**
     * 获取模型描述信息的国际化数组，支持多语言版本。
     * @return 包含多个语言版本描述信息的字符串数组
     */
    String[] infoI18n();

    /**
     * 获取模型的入口操作，默认为列表动作。
     * @return 入口操作名称
     * @see Listable#ACTION_NAME_LIST
     */
    String entryAction() default Listable.ACTION_NAME_LIST;

    /**
     * 判断模型是否应在菜单中显示，默认为true。
     * @return 如果模型应显示在菜单中，则返回true，否则返回false
     */
    boolean showToMenu() default true;

    /**
     * 获取模型所属的菜单名称。
     * @return 模型所属的菜单名称
     */
    String menuName() default "";

    /**
     * 获取模型在菜单中的排序顺序，数值越小越靠前，默认为0。
     * @return 菜单排序顺序整数值
     */
    int menuOrder() default 0;
}

