package qingzhou.api;

import java.util.Map;

/**
 * 代表客户端请求相关信息
 */
public interface Request {
    Response getResponse();

    String getModel();

    String getAction();

    String getId();

    String getParameter(String name);

    // 只读视图：参数由平台在输入边界一次性解析写入，应用侧回写会绕过配额与校验
    Map<String, String> getParameters();
}
