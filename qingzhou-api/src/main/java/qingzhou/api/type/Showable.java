package qingzhou.api.type;

import java.util.Map;

/**
 * Showable接口定义了展示数据的能力。
 *
 */
public interface Showable {
    // 定义展示操作的常量名称
    String ACTION_NAME_SHOW = "show";

    /**
     * 根据指定的id展示数据。
     * @param id 用于标识要展示的数据的唯一标识符。
     * @return 返回一个包含展示数据相关信息的Map。默认情况下返回null。
     */
    default Map<String, String> showData(String id) {
        return null;
    }
}
