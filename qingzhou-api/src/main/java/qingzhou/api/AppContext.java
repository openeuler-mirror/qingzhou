package qingzhou.api;

import qingzhou.api.metadata.AppMetadata;

import java.io.File;
import java.util.Collection;

/**
 * 应用上下文接口，提供应用元数据、平台信息获取，以及服务定位、临时文件管理、国际化信息、菜单管理和动作过滤器管理等功能。
 */
public interface AppContext {
    /**
     * 获取应用元数据。
     *
     * @return AppMetadata 应用元数据对象，包含应用的基本信息。
     */
    AppMetadata getAppMetadata();

    /**
     * 获取平台名称。
     *
     * @return String 平台的名称。
     */
    String getPlatformName();

    /**
     * 获取平台版本。
     *
     * @return String 平台的版本信息。
     */
    String getPlatformVersion();

    /**
     * 获取所有框架提供的公共服务的集合。
     *
     * @return Collection<Class<?>> 包含所有服务类型Class对象的集合。
     */
    Collection<Class<?>> getServiceTypes();

    /**
     * 根据服务类型获取服务实例。
     *
     * @param <T> 服务类型。
     * @param serviceType 服务的Class对象。
     * @return T 该服务类型的实例。
     */
    <T> T getService(Class<T> serviceType);

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
     * @param key 国际化信息的键。
     * @param i18n 国际化信息的值数组。
     */
    void addI18n(String key, String[] i18n);

    /**
     * 添加菜单项。
     *
     * @param menuName 菜单项的名称。
     * @param menuI18n 菜单项的国际化信息数组。
     * @param menuIcon 菜单项的图标。
     * @param menuOrder 菜单项的顺序。
     */
    void addMenu(String menuName, String[] menuI18n, String menuIcon, int menuOrder);

    /**
     * 设置默认数据存储。
     *
     * @param dataStore 默认数据存储实例。
     */
    void setDefaultDataStore(DataStore dataStore);

    /**
     * 获取默认数据存储。
     *
     * @return DataStore 默认数据存储实例。
     */
    DataStore getDefaultDataStore();
}

