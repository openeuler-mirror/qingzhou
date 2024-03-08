package qingzhou.api;

/**
 * 请求接口定义了获取请求相关信息的方法。
 */
public interface Request {

    /**
     * 获取应用程序名称。
     * @return 返回应用程序的名称。
     */
    String getAppName();

    /**
     * 获取模型名称。
     * @return 返回模型的名称。
     */
    String getModelName();

    /**
     * 获取操作名称。
     * @return 返回操作的名称。
     */
    String getActionName();

    /**
     * 获取视图名称。
     * @return 返回视图的名称。
     */
    String getViewName();

    /**
     * 获取请求的ID。
     * @return 返回请求的唯一标识符。
     */
    String getId();

    /**
     * 获取指定参数的值。
     * @param parameterName 参数的名称。
     * @return 返回参数的值，如果不存在，则返回null。
     */
    String getParameter(String parameterName);

    /**
     * 获取用户名。
     * @return 返回当前请求的用户名。
     */
    String getUserName();

    /**
     * 获取国际化语言设置。
     * @return 返回当前的国际化语言设置。
     */
    Lang getI18nLang();
}

