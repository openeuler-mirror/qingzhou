package qingzhou.api.action;

import java.util.Map;

public interface Update {
    String ACTION_CODE_UPDATE = "update";

    void update(String id, Map<String, String> data) throws Exception;
}
