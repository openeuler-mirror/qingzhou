package qingzhou.logger.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import qingzhou.common.Configs;
import qingzhou.logger.Logger;
import qingzhou.logger.LoggerProvider;

/**
 * 多命名日志器：单实例组件按 qingzhou-logger.loggers 构建日志器目录。
 *
 * <p>只有默认日志器以 {@link Logger} 服务对外暴露，其余按名称经 {@link LoggerProvider} 获取：
 * 若每个命名日志器都注册 Logger 服务，框架停止期默认日志器先注销时，无 target 的
 * {@code @Reference Logger} 会重绑到其余实例，服务器日志便混入应用日志 writer。</p>
 */
@Component(service = {Logger.class, LoggerProvider.class},
        configurationPid = "qingzhou-logger", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class LoggerImpl implements Logger, LoggerProvider {
    static final String DEFAULT_LOGGER = "default";
    private static final long DEFAULT_THROTTLE_WINDOW_MS = 30_000L;

    // init 期间填充，之后只读：可见性由 DS 激活发布服务时的 happens-before 保证
    private final Map<String, Logger> namedLoggers = new HashMap<>();
    private Logger defaultLogger;
    private ShowInfo showInfo;

    @Activate
    public void init(Map<String, Object> config) {
        Map<String, String> props = toStringMap(config);
        Configs configs = new Configs(props);

        List<String> names = configs.splitList("loggers");
        if (names.isEmpty())
            throw new IllegalArgumentException("qingzhou-logger.loggers must list at least the " + DEFAULT_LOGGER);
        for (String name : names) validateName(name);

        TinylogBootstrap.apply(props, names);
        for (String name : names) {
            namedLoggers.put(name, new TinylogLogger(name, new LogThrottle(
                    configs.atLeast(name + ".throttle_window_ms", 1, DEFAULT_THROTTLE_WINDOW_MS))));
        }

        defaultLogger = namedLoggers.get(DEFAULT_LOGGER);
        if (defaultLogger == null)
            throw new IllegalArgumentException("qingzhou-logger.loggers must contain " + DEFAULT_LOGGER);

        showInfo = new ShowInfo(this);
        showInfo.activate();
    }

    @Deactivate
    public void shutdown() {
        if (showInfo != null) showInfo.deactivate();
        try {
            org.tinylog.provider.ProviderRegistry.getLoggingProvider().shutdown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public Logger getLogger(String name) {
        Logger logger = namedLoggers.get(name);
        return logger != null ? logger : defaultLogger; // 未配置该名称时回退默认日志器
    }

    // 契约适配：CM 属性值为 Object，非 String 项（如 service.ranking）不属于日志配置
    private static Map<String, String> toStringMap(Map<String, Object> config) {
        Map<String, String> props = new HashMap<>();
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            if (entry.getValue() instanceof String) props.put(entry.getKey(), (String) entry.getValue());
        }
        return props;
    }

    // 名称进入 tinylog 的 writer 键与 tag：含 . , 空白或值为 - 会使 writer 键无法识别、路由错位，在边界一次性失败
    private static void validateName(String name) {
        if (!name.matches("[A-Za-z0-9_]+"))
            throw new IllegalArgumentException("Invalid logger name: " + name + " (only letters, digits, and underscores are allowed)");
    }

    // Logger 接口委托默认日志器：平台组件不感知命名日志器
    @Override
    public boolean isDebugEnabled() {
        return defaultLogger.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        defaultLogger.debug(msg);
    }

    @Override
    public void debug(String msg, Throwable t) {
        defaultLogger.debug(msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return defaultLogger.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        defaultLogger.info(msg);
    }

    @Override
    public void info(String msg, Throwable t) {
        defaultLogger.info(msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return defaultLogger.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        defaultLogger.warn(msg);
    }

    @Override
    public void warn(String msg, Throwable t) {
        defaultLogger.warn(msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return defaultLogger.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        defaultLogger.error(msg);
    }

    @Override
    public void error(String msg, Throwable t) {
        defaultLogger.error(msg, t);
    }
}
