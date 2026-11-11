package qingzhou.api;

import java.io.File;
import java.util.Map;

public interface AppContext {
    Map<String, String> getAppConfig();

    // 自动探测到的路径，在 MANIFEST 中配置了 Qingzhou-Detection-Feature-Files 等参数后有效
    String getDetectedPath();

    File getRoot();

    File getTempDir();

    /**
     * 获取平台提供的共享服务，如：qingzhou.logger.Logger、qingzhou.json.Json等。
     */
    <T> T getService(Class<T> type);

    /**
     * 获取平台提供的指定名字的共享服务，如：qingzhou.jdbc.JdbcPool 的多实例服务。
     */
    <T> T getService(Class<T> type, String name);

    void addActionFilter(ActionFilter... actionFilter);

    long getPid();
}
