package qingzhou.api;

import java.util.Map;

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

    Map<String, String> getParameters();
}
