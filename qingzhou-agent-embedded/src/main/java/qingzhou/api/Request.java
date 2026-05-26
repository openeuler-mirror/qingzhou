package qingzhou.api;

/**
 * 代表客户端请求相关信息
 */
public interface Request {
    Response getResponse();

    String getInstance();

    String getApp();

    String getModel();

    String getAction();

    String getId();

    String getParameter(String name);

    java.util.Map<String, String> getParameters();
}