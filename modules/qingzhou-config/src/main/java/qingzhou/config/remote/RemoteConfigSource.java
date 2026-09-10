package qingzhou.config.remote;

import java.util.Map;

/**
 * 远程配置中心统一抽象。配置中心被隔离在接口之后，主流程不依赖任何具体实现符号，
 * 便于后续接入 Nacos、Zookeeper 等其它配置中心。
 */
public interface RemoteConfigSource {
    /**
     * 拉取指定命名空间下的全部模块配置文档。
     *
     * @param namespace 数据隔离命名空间，如 qingzhou/default/tenant/instance
     * @return pid -> (pid 内部 key -> value)
     */
    Map<String, Map<String, String>> pull(String namespace) throws Exception;
}
