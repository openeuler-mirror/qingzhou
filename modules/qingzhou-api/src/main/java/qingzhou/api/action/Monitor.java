package qingzhou.api.action;

import java.util.Map;

public interface Monitor {
    String ACTION_CODE_MONITOR = "monitor";

    Map<String, String> monitor(String id) throws Exception;
}
