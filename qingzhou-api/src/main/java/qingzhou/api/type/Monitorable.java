package qingzhou.api.type;

import java.util.Map;

public interface Monitorable {
    String ACTION_NAME_MONITOR = "monitor";

    String MONITOR_EXT_SEPARATOR = ":";

    Map<String, String> monitorData();
}
