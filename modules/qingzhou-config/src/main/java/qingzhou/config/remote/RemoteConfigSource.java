package qingzhou.config.remote;

import java.util.Map;

/**
 * 远程配置中心统一抽象：具体实现被隔离在接口之后，便于后续接入 Nacos、Zookeeper。
 */
public interface RemoteConfigSource {
    /** @return pid -> (pid 内部 key -> value)，无数据时返回空集合 */
    Map<String, Map<String, String>> pull() throws Exception;
}
