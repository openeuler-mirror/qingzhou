package qingzhou.api;

import java.util.Map;

/**
 * 请求接口定义了获取请求相关信息的方法。
 */
public interface Request {

    /**
     * 获取应用程序名称。
     *
     * @return 返回应用程序的名称。
     */
    String getAppName();

    /**
     * 获取模型名称。
     *
     * @return 返回模型的名称。
     */
    String getModelName();

    /**
     * 获取操作名称。
     *
     * @return 返回操作的名称。
     */
    String getActionName();

    /**
     * 获取视图名称。
     *
     * @return 返回视图的名称。
     */
    String getViewName();

    /**
     * 获取请求的ID。
     *
     * @return 返回请求的唯一标识符。
     */
    String getId();

    /**
     * Returns the value of a request parameter as a <code>String</code>, or
     * <code>null</code> if the parameter does not exist. Request parameters are
     * extra information sent with the request. For HTTP servlets, parameters
     * are contained in the query string or posted form data.
     * <p>
     * You should only use this method when you are sure the parameter has only
     * one value. If the parameter might have more than one value, use
     * {@link #getParameterMap}.
     */
    String getParameter(String name);

    /**
     * Returns a java.util.Map of the parameters of this request. Request
     * parameters are extra information sent with the request. For HTTP
     * servlets, parameters are contained in the query string or posted form
     * data.
     *
     * @return an immutable java.util.Map containing parameter names as keys and
     * parameter values as map values. The keys in the parameter map are
     * of type String. The values in the parameter map are of type
     * String array.
     */
    Map<String, String[]> getParameterMap();

    /**
     * 获取用户名。
     *
     * @return 返回当前请求的用户名。
     */
    String getUserName();

    /**
     * 获取国际化语言设置。
     *
     * @return 返回当前的国际化语言设置。
     */
    Lang getI18nLang();
}

