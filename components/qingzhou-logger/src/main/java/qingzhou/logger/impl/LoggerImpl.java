package qingzhou.logger.impl;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.*;
import qingzhou.logger.Logger;

/**
 * 多日志器实现：支持 qingzhou-logger~<名称>.* 配置多个命名日志器，
 * 每个日志器的 writer 通过 tinylog tag 路由到独立文件。
 * tinylog 全局配置在首次激活时一次性合并 apply（其 2.6.2 之后配置冻结不可变更），
 * 因此运行期新增的日志器配置须重启生效。
 */
@Component(configurationPid = "qingzhou-logger", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class LoggerImpl implements Logger {
    static final String DEFAULT_LOGGER = qingzhou.common.Constants.CONFIG_FACTORY_DEFAULT_INSTANCE;
    private static final long DEFAULT_THROTTLE_WINDOW_MS = 30_000L; // 同一失败消息的聚合窗口默认值

    // 非工厂 pid（如 qingzhou-logger）即单实例默认日志器
    static String parseLoggerName(String pid) {
        int index = pid.indexOf('~');
        return index == -1 ? DEFAULT_LOGGER : pid.substring(index + 1);
    }

    private static final Set<String> activePids = ConcurrentHashMap.newKeySet();

    private org.tinylog.TaggedLogger tinylog;
    private LogThrottle throttle;

    @Reference
    private ConfigurationAdmin configAdmin;

    private String pid;
    private ShowInfo showInfo;

    @Activate
    public void init(Map<String, Object> config) throws Exception {
        pid = (String) config.get(Constants.SERVICE_PID);
        activePids.add(pid);
        throttle = new LogThrottle(throttleWindow(config));

        String loggerName = parseLoggerName(pid);

        initTinylog(loggerName);

        // 仅默认日志器输出启动 banner，避免多日志器重复打印
        if (DEFAULT_LOGGER.equals(loggerName)) {
            showInfo = new ShowInfo(this);
            showInfo.activate();
        }
    }

    // 聚合窗口可经外部配置（throttle_window_ms）调整，未配置取默认 30 秒
    private static long throttleWindow(Map<String, Object> config) {
        Object value = config.get("throttle_window_ms");
        return value == null ? DEFAULT_THROTTLE_WINDOW_MS : Long.parseLong(value.toString().trim());
    }

    private void initTinylog(String loggerName) throws Exception {
        // 先 apply 再触碰 org.tinylog.Logger：其类初始化会加载 provider 并冻结配置
        TinylogBootstrap.apply(configAdmin);
        tinylog = org.tinylog.Logger.tag(loggerName);
    }

    @Deactivate
    public void shutdown() {
        if (showInfo != null) showInfo.deactivate();

        activePids.remove(pid);
        if (activePids.isEmpty()) {
            try {
                org.tinylog.provider.ProviderRegistry.getLoggingProvider().shutdown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return tinylog.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        tinylog.debug(msg);
    }

    @Override
    public void debug(String msg, Throwable t) {
        tinylog.debug(t, msg);
    }

    @Override
    public boolean isInfoEnabled() {
        return tinylog.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        tinylog.info(msg);
    }

    @Override
    public void info(String msg, Throwable t) {
        tinylog.info(t, msg);
    }

    @Override
    public boolean isWarnEnabled() {
        return tinylog.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        throttled('W', msg, tinylog::warn);
    }

    @Override
    public void warn(String msg, Throwable t) {
        throttled('W', msg, m -> tinylog.warn(t, m));
    }

    @Override
    public boolean isErrorEnabled() {
        return tinylog.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        throttled('E', msg, tinylog::error);
    }

    @Override
    public void error(String msg, Throwable t) {
        throttled('E', msg, m -> tinylog.error(t, m));
    }

    // 限流：同一级别 + 消息在窗口内只放行一次，并附带被抑制的条数
    private void throttled(char level, String msg, Consumer<String> log) {
        long suppressed = throttle.acquire(level, msg);
        if (suppressed >= 0) log.accept(suppressed > 0 ? msg + " (suppressed " + suppressed + " similar)" : msg);
    }
}
