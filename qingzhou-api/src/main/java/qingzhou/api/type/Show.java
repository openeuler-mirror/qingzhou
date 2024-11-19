package qingzhou.api.type;

import java.util.Map;

/**
 * Show接口定义了展示数据的能力。
 */
public interface Show {
    String ACTION_SHOW = "show";

    Map<String, String> showData(String id) throws Exception;
}
