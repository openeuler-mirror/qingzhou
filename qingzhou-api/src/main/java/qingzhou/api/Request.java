package qingzhou.api;

import java.util.Enumeration;

/**
 * 请求接口定义了获取请求相关信息的方法。
 */
public interface Request {

    /**
     * 获取应用程序名称。
     *
     * @return 返回应用程序的名称。
     */
    String getApp();

    /**
     * 获取模型名称。
     *
     * @return 返回模型的名称。
     */
    String getModel();

    /**
     * 获取操作名称。
     *
     * @return 返回操作的名称。
     */
    String getAction();

    /**
     * 获取视图名称。
     *
     * @return 返回视图的名称。
     */
    String getView();

    /**
     * 获取请求的ID。
     */
    String getId();

    /**
     * 批量处理时的id组
     */
    String[] getBatchId();

    /**
     * Returns the value of a request parameter as a <code>String</code>, or
     * <code>null</code> if the parameter does not exist.
     * 多值须以英文逗号（,）分割
     */
    String getParameter(String name);

    /**
     * 使用 Java 自省技术，创建对象，并将请求参数赋给对象，以便于通过对象方式获取请求参数
     */
    <T> T getParameterAsObject(Class<T> objectType) throws Exception;

    Enumeration<String> getParameterNames();

    Parameters parametersForSession();

    Parameters parametersForSubMenu();

    String getHeader(String name);

    Enumeration<String> getHeaderNames();

    /**
     * 获取 http 请求消息体
     * 需要首先设置 ModelAction.request_body = true
     */
    byte[] getHttpBody();

    /**
     * 获取用户名。
     *
     * @return 返回当前请求的用户名。
     */
    String getUser();

    /**
     * 获取国际化语言设置。
     *
     * @return 返回当前的国际化语言设置。
     */
    Lang getLang();

    Response getResponse();

    interface Parameters {
        String put(String key, String value);

        void putAll(Parameters parameters);

        String get(String key);

        String remove(String key);
    }
}
