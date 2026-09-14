package qingzhou.kv;

import java.util.function.UnaryOperator;

/**
 * 通用键值存储契约：namespace + key → JSON 文本值（编解码归消费方）。
 * <p>
 * 原子性：{@link #update} 为同键互斥的原子读改写原语，追加/计数等读改写竞态必须经其完成；
 * key/namespace 受契约正则约束（路径安全底线），单值上限 {@link #MAX_VALUE_BYTES} 字节。
 * 实现注册为 OSGi 服务，service property type=file|memory 区分后端，消费方按需绑定。
 */
public interface KeyValueStore {
    /** key 合法字符（路径安全底线：拒绝 ..、/ 等） */
    String VALID_KEY = "[A-Za-z0-9_-]{1,128}";
    String VALID_NAMESPACE = "[a-z0-9][a-z0-9-]{0,31}";
    /** 单值上限（UTF-8 字节），超限拒绝：截断 JSON 会破坏结构 */
    int MAX_VALUE_BYTES = 256 * 1024;

    /** 取值（UTF-8 JSON 文本），键不存在返回 null */
    String get(String namespace, String key);

    /** 写入（覆盖同键旧值） */
    void put(String namespace, String key, String value);

    /**
     * 原子读改写（同键互斥，不同键并行）：fn 入参为当前值（键不存在时为 null），
     * 返回新值写回，返回 null 表示删除该键；方法返回 fn 的结果（删除时为 null）。
     */
    String update(String namespace, String key, UnaryOperator<String> fn);

    boolean delete(String namespace, String key);

    boolean exists(String namespace, String key);
}
