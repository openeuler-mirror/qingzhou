package qingzhou.api.type;

import java.util.Map;

/**
 * 监控接口，定义了监控数据的获取方法。
 * 该接口用于让实现类提供监控数据，以供监控系统收集和处理。
 */
public interface Monitorable {
    // 监控操作的常量名称。
    String ACTION_NAME_MONITOR = "monitor";

    // 监控数据扩展字段的分隔符，用于在监控数据键值对中区分不同的扩展字段。
    String MONITOR_EXT_SEPARATOR = ":";

    /**
     * 获取监控数据。
     *
     * @return 返回一个包含监控数据的键值对集合。每个键值对代表一个监控数据项，
     *         其中键表示监控数据的名称，值表示监控数据的当前值。
     */
    Map<String, String> monitorData();
}

