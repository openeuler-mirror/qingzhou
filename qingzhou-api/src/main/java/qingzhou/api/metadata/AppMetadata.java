package qingzhou.api.metadata;

import qingzhou.api.Lang;

import java.util.Map;

/**
 * 应用元数据接口，定义了应用的基本信息获取方法。
 */
public interface AppMetadata {
    /**
     * 获取应用名称。
     *
     * @return 返回应用的名称，类型为String。
     */
    String getName();

    /**
     * 获取应用的配置信息。
     *
     * @return 返回一个Map对象，包含应用的配置信息。
     */
    Map<String, String> getConfig();

    /**
     * 根据语言和键值获取国际化信息。
     *
     * @param lang 语言对象，指定获取哪种语言的国际化信息。
     * @param key 需要获取的国际化信息的键。
     * @param args 如果国际化信息中包含占位符，这里可以传入替换的参数。
     * @return 返回对应语言和键值的国际化信息字符串。
     */
    String getI18n(Lang lang, String key, Object... args);

    /**
     * 根据菜单名获取菜单数据。
     *
     * @param menuName 菜单的名称，用于指定需要获取的菜单。
     * @return 返回一个MenuData对象，包含指定菜单的数据。
     */
    MenuData getMenu(String menuName);

    /**
     * 获取模型管理器。
     *
     * @return 返回一个ModelManager对象，用于管理应用的模型。
     */
    ModelManager getModelManager();
}

