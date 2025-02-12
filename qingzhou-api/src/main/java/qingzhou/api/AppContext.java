package qingzhou.api;

import java.io.File;
import java.util.Properties;

/**
 * 应用上下文接口，提供应用元数据、平台信息获取，以及服务定位、临时文件管理、国际化信息、菜单管理和动作过滤器管理等功能。
 */
public interface AppContext {
    String APP_HOME_MODEL = "home";

    // 应用自带的 "qingzhou.properties" 文件
    Properties getAppProperties();

    Request getThreadLocalRequest();

    // 在异步子线程中使用 AppContext 对象前，需首先从父线程里 getThreadLocalRequest() 获得当前 Request，并调用 setThreadLocalRequest(Request request) 设置到子线程中
    void setThreadLocalRequest(Request request);

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
    void addAppActionFilter(ActionFilter... actionFilter);

    // 为指定模块添加过滤器
    void addModelActionFilter(ModelBase modelBase, ActionFilter... actionFilter);

    /**
     * 设置轻舟使用应用自定义的登录认证插件，拦截处理轻舟的登陆流程。
     * 注意：设置的插件仅在应用安装到本地实例上时有效！如果多个应用设置，则随机选择一个。
     */
    void setAuthAdapter(AuthAdapter authAdapter);

    /**
     * 添加国际化信息。
     *
     * @param key  国际化信息的键。
     * @param i18n 国际化信息的值数组。
     */
    void addI18n(String key, String[] i18n);

    String getI18n(Lang lang, String key, Object... args);

    /**
     * 根据 getRequestLang() 返回值查找 i18n
     */
    String getI18n(String key, Object... args);

    /**
     * 添加菜单项，根据返回的 Menu 对象，设置菜单的细节。
     * 注：在启动过程中调用有效
     */
    Menu addMenu(String name, String[] i18n);

    /**
     * 获取本Module开放的Service，以及从其它Module注入的Service
     */
    <T> T getService(Class<T> clazz);

    void invokeSuperAction(Request request) throws Exception;

    String getPlatformVersion();

    // 传递外部参数到应用内部，用于业务逻辑需求
    String[] getStartArgs();
}
