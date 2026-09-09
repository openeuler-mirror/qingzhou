package qingzhou.config.remote;

import java.util.Locale;

import qingzhou.config.remote.etcd.EtcdConfigSource;

/** 按 remote.type 创建配置中心实现；未支持的类型明确报错，不静默降级。 */
public final class RemoteConfigSourceFactory {
    private RemoteConfigSourceFactory() {
    }

    public static RemoteConfigSource create(RemoteOptions options) {
        String type = options.type == null ? "etcd" : options.type.trim();
        if (type.toLowerCase(Locale.ROOT).equals("etcd")) {
            return new EtcdConfigSource(options);
        }
        throw new IllegalArgumentException("unsupported remote config center type: " + type);
    }
}
