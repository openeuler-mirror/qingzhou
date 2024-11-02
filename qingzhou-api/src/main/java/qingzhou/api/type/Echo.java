package qingzhou.api.type;

import java.util.Map;

/**
 * 表单数据联动
 */
public interface Echo {
    String ACTION_ECHO = "echo";

    Map<String, String> echoData(String echoGroup, Map<String, String> params) throws Exception;
}
