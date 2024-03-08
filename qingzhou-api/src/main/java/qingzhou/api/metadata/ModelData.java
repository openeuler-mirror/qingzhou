package qingzhou.api.metadata;

/**
 * ModelData 接口定义了模型数据的基本属性和行为。
 * 该接口主要用于定义一个模型的元数据，包括名称、图标、国际化名称、国际化信息、入口动作、菜单显示控制、菜单名称和菜单顺序。
 */
public interface ModelData {
    /**
     * 获取模型的名称。
     * @return 返回模型的名称，通常是一个字符串。
     */
    String name();

    /**
     * 获取模型的图标名称。
     * @return 返回模型图标的名称。
     */
    String icon();

    /**
     * 获取模型名称的国际化字符串数组。
     * @return 返回一个包含模型名称的国际化字符串的数组。
     */
    String[] nameI18n();

    /**
     * 获取模型描述信息的国际化字符串数组。
     * @return 返回一个包含模型描述信息的国际化字符串的数组。
     */
    String[] infoI18n();

    /**
     * 获取模型的入口动作。
     * @return 返回一个表示模型入口动作的字符串，通常是一个方法名。
     */
    String entryAction();

    /**
     * 判断模型是否显示在菜单中。
     * @return 如果模型应该在菜单中显示，则返回true；否则返回false。
     */
    boolean showToMenu();

    /**
     * 获取模型所属的菜单名称。
     * @return 返回模型所属的菜单名称。
     */
    String menuName();

    /**
     * 获取模型在菜单中的显示顺序。
     * @return 返回一个整数，表示模型在菜单中的显示顺序。
     */
    int menuOrder();
}

