package qingzhou.api;

import java.util.Enumeration;
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
     * {@link #getParameterValues}.
     * <p>
     * If you use this method with a multivalued parameter, the value returned
     * is equal to the first value in the array returned by
     * <code>getParameterValues</code>.
     * <p>
     *
     * @param name a <code>String</code> specifying the name of the parameter
     * @return a <code>String</code> representing the single value of the
     * parameter
     * @see #getParameterValues
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
     * Returns an <code>Enumeration</code> of <code>String</code> objects
     * containing the names of the parameters contained in this request. If the
     * request has no parameters, the method returns an empty
     * <code>Enumeration</code>.
     *
     * @return an <code>Enumeration</code> of <code>String</code> objects, each
     * <code>String</code> containing the name of a request parameter;
     * or an empty <code>Enumeration</code> if the request has no
     * parameters
     */
    Enumeration<String> getParameterNames();

    /**
     * Returns an array of <code>String</code> objects containing all of the
     * values the given request parameter has, or <code>null</code> if the
     * parameter does not exist.
     * <p>
     * If the parameter has a single value, the array has a length of 1.
     *
     * @param name a <code>String</code> containing the name of the parameter
     *             whose value is requested
     * @return an array of <code>String</code> objects containing the parameter's
     * values
     * @see #getParameter
     */
    String[] getParameterValues(String name);

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

