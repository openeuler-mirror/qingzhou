package qingzhou.api.type;


import java.util.Map;

public interface Echo {
    String ACTION_ECHO = "echo";

    Map<String, String> echoData(String echoGroup, Map<String, String> params);
}
