package qingzhou.api.type;

import java.util.Map;

/**
 * Showable接口定义了展示数据的能力。
 */
public interface Showable {
    String ACTION_SHOW = "show";

    Map<String, String> showData(String id) throws Exception;
}
