package qingzhou.framework;

import java.util.List;
import java.util.Map;

public interface ConfigManager {
    Map<String, String> getConfig(String xpath);

    List<Map<String, String>> getConfigList(String xpath);
}
