package qingzhou.api;

import java.io.File;

/**
 * 应用上下文接口，提供应用元数据、平台信息获取，以及服务定位、临时文件管理、国际化信息、菜单管理和动作过滤器管理等功能。
 */
public interface AppContext {
    /**
     * 获取应用程序目录的文件对象。
     *
     * @return 返回表示应用程序目录的File对象。如果无法确定或创建应用程序目录，则返回null。
     */
    File getAppDir();

    /**
     * 获取临时文件目录。
     *
     * @return File 临时文件目录的File对象。
     */
    File getTemp();

    /**
     * 添加操作过滤器。
     *
     * @param actionFilter 操作过滤器实例。
     */
    void addActionFilter(ActionFilter actionFilter);

    /**
     * 添加国际化信息。
     *
     * @param key  国际化信息的键。
     * @param i18n 国际化信息的值数组。
     */
    void addI18n(String key, String[] i18n);

    String getI18n(Lang lang, String key, Object... args);

    /**
     * 添加菜单项。
     *
     * @param name  菜单项的名称。
     * @param i18n  菜单项的国际化信息数组。
     * @param icon  菜单项的图标。
     * @param order 菜单项的顺序。
     */
    void addMenu(String name, String[] i18n, String icon, int order);
}
