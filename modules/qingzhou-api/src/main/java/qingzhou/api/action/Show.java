package qingzhou.api.action;

import java.util.Map;

public interface Show {
    String ACTION_CODE_SHOW = "show";

    Map<String, String> show(String id) throws Exception;
}
