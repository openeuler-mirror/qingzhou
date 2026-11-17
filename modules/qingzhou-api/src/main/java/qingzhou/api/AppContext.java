package qingzhou.api;

import java.io.File;
import java.util.Map;

/**
 * 应用运行期上下文，由平台创建并注入 {@link QingzhouApp#start(AppContext)} 与 {@link ModelBase}，
 * 是应用访问配置、目录与平台共享服务的唯一通道。
 */
public interface AppContext {
    /**
     * 只读的应用配置，来自实例配置中的应用条目；{@code code} 键可覆盖 {@code @App.code}。
     */
    Map<String, String> getAppConfig();

    /**
     * 自动探测到的目标软件安装目录，需在 MANIFEST 中声明探测特征文件后才有效，未探测到返回 {@code null}。
     */
    String getDetectedPath();

    /**
     * 实例根目录。
     */
    File getRoot();

    /**
     * 应用私有临时目录，位于实例根目录下，由框架负责清理，不可存放需要长期保留的数据。
     */
    File getTempDir();

    /**
     * 获取平台提供的共享服务，如：qingzhou.logger.Logger、qingzhou.json.Json；未注册返回 {@code null}。
     */
    <T> T getService(Class<T> type);

    /**
     * 获取指定名字的共享服务，用于工厂化多实例服务，如：qingzhou.jdbc.JdbcPool 的各数据源实例。
     * <p>
     * 名称既匹配服务的 {@code service.pid}，也匹配工厂配置中 {@code pid~实例名} 的实例名。
     */
    <T> T getService(Class<T> type, String name);

    /**
     * 注册应用级请求过滤器，可多次调用，按注册顺序执行。链固定为：平台内置校验 → 应用过滤器 → 目标动作；
     * 启动后新增的过滤器立即生效。
     */
    void addActionFilter(ActionFilter... actionFilter);
}
