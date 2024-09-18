package qingzhou.api.type;

import qingzhou.api.Groups;

import java.util.Map;

/**
 * Showable接口定义了展示数据的能力。
 */
public interface Showable {
    String ACTION_SHOW = "show";

    Map<String, String> showData(String id) throws Exception;

    // 字段分组
    default Groups groups() {
        return null;
    }
}
