package qingzhou.api.metadata;

/**
 * MenuData接口定义了菜单项的数据模型。
 * 该接口用于获取菜单项的名称、国际化字符串数组、图标以及排序顺序。
 */
public interface MenuData {
    /**
     * 获取菜单项的名称。
     *
     * @return 菜单项的名称，通常为字符串。
     */
    String getName();

    /**
     * 获取菜单项的国际化字符串数组。
     *
     * @return 包含菜单项名称的国际化字符串的数组。
     */
    String[] getI18n();

    /**
     * 获取菜单项的图标。
     *
     * @return 菜单项图标的路径或编码，形式为字符串。
     */
    String getIcon();

    /**
     * 获取菜单项的排序顺序。
     *
     * @return 菜单项的排序顺序，为整数类型。
     */
    int getOrder();
}

