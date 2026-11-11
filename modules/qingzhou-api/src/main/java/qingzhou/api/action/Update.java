package qingzhou.api.action;

import java.util.Map;

public interface Update {
    void update(String id, Map<String, String> data) throws Exception;
}
