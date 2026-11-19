package qingzhou.api.action;

import java.util.Map;

/**
 * 实时监视的能力，对应内置动作 {@code monitor}。
 * <p>
 * 返回值经 {@code field_type=monitor} 白名单过滤后交给管控台；其中 {@code numeric=true} 的字段还会被导出为
 * 监控指标，取值必须是纯十进制数值字符串，采集不到时应省略该 key，不得填 {@code -1} 之类的哨兵值。
 */
public interface Monitor {
    /**
     * @param id 被监视对象标识，无对象级区分的模块可忽略
     * @return key 为 {@code @ModelField.code}，非监视字段会被丢弃
     */
    Map<String, String> monitor(String id) throws Exception;
}
