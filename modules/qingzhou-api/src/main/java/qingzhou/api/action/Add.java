package qingzhou.api.action;

import java.util.Map;

public interface Add {
    String ACTION_CODE_ADD = "add";

    void add(Map<String, String> data) throws Exception;
}
