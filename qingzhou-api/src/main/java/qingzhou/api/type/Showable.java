package qingzhou.api.type;

import java.util.Map;

/**
 * Showable接口定义了展示数据的能力。
 */
public interface Showable {
    default boolean exists(String id) throws Exception {
        Map<String, String> data = showData(id);
        return data != null && !data.isEmpty();
    }

    Map<String, String> showData(String id) throws Exception;
}
