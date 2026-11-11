package qingzhou.api.action;

import java.util.Map;

public interface Monitor {
    Map<String, String> monitor(String id) throws Exception;
}
