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

    // 获取未使用 @ModelField 标注的参数
    String getNonModelParameter(String name);

    /**
     * Returns the value of a request parameter as a <code>String</code>, or
     * <code>null</code> if the parameter does not exist.
     * 多值须以英文逗号（,）分割
     */
    String getParameter(String name);

    Enumeration<String> getParameterNames();

    String getParameterInSession(String name);

    void setParameterInSession(String key, String val);

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
}
