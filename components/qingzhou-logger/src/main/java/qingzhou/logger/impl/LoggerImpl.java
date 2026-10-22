package qingzhou.logger.impl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
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
    private static final String DEFAULT_TAG = qingzhou.common.Constants.CONFIG_FACTORY_DEFAULT_INSTANCE;
    private static final Set<String> activePids = ConcurrentHashMap.newKeySet();
    private static final Set<String> appliedTags = ConcurrentHashMap.newKeySet();
    static volatile boolean applied; // tinylog 全局配置仅可 apply 一次，测试可直接置位跳过

    private final LogThrottle throttle = new LogThrottle();

    @Reference
    private ConfigurationAdmin configAdmin;

    private String tag = DEFAULT_TAG;
    private String pid;
    private ShowInfo showInfo;

    @Activate
    public void init(Map<String, Object> config) {
        pid = (String) config.get(Constants.SERVICE_PID);
        tag = parseTag(pid);
        activePids.add(pid != null ? pid : DEFAULT_TAG);

        ensureApplied(config);

        // 仅默认日志器输出启动 banner，避免多日志器重复打印
        if (DEFAULT_TAG.equals(tag)) {
            showInfo = new ShowInfo(this);
            showInfo.activate();
        }
    }

    private static String parseTag(String pid) {
        if (pid == null) return DEFAULT_TAG;
        int i = pid.indexOf('~');
        return i == -1 ? DEFAULT_TAG : pid.substring(i + 1);
    }

    private void ensureApplied(Map<String, Object> ownConfig) {
        synchronized (LoggerImpl.class) {
            if (applied) {
                if (!appliedTags.contains(tag)) {
                    System.err.println("logger '" + tag + "' configured after tinylog initialization, restart required to take effect");
                }
                return;
            }
            applied = true;
            org.tinylog.configuration.Configuration.replace(merge(gatherConfigs(ownConfig)));
        }
    }

    // 收集全部日志器配置：tag -> 该日志器的字符串配置（全局项 + writer 项）
    private Map<String, Map<String, String>> gatherConfigs(Map<String, Object> ownConfig) {
        Map<String, Map<String, String>> result = new HashMap<>();
        try {
            Configuration[] configs = configAdmin == null ? null
                    : configAdmin.listConfigurations("(service.pid=qingzhou-logger*)");
            if (configs != null) {
                for (Configuration c : configs) {
                    Dictionary<String, Object> properties = c.getProperties();
                    if (properties == null) continue;
                    Map<String, String> map = new HashMap<>();
                    for (java.util.Enumeration<String> keys = properties.keys(); keys.hasMoreElements(); ) {
                        String key = keys.nextElement();
                        Object value = properties.get(key);
                        if (value instanceof String) map.put(key, (String) value);
                    }
                    result.put(parseTag(c.getPid()), map);
                }
            }
        } catch (Exception e) {
            System.err.println("failed to list logger configurations: " + e.getMessage());
        }
        // 兜底：确保自身配置一定参与合并（ConfigAdmin 事件时序异常时不至于丢失当前日志器）
        Map<String, String> own = new HashMap<>();
        ownConfig.forEach((k, v) -> {
            if (v instanceof String) own.put(k, (String) v);
        });
        result.put(tag, own);
        return result;
    }

    /**
     * 合并所有日志器的配置为一份 tinylog 全局配置。
     * 全局项（level、writingthread 等非 writer 键）仅取默认日志器；
     * writer 项合并所有日志器，命名日志器的 writer 重命名并附加 tag。
     */
    static Map<String, String> merge(Map<String, Map<String, String>> perTagConfigs) {
        appliedTags.addAll(perTagConfigs.keySet());

        Map<String, String> merged = new HashMap<>();
        merged.put("autoshutdown", "false");

        Map<String, String> defaultConfig = perTagConfigs.get(DEFAULT_TAG);
        if (defaultConfig != null) {
            for (Map.Entry<String, String> entry : defaultConfig.entrySet()) {
                if (!entry.getKey().startsWith("writer")) {
                    merged.put(entry.getKey(), entry.getValue());
                }
            }
        }

        for (Map.Entry<String, Map<String, String>> entry : perTagConfigs.entrySet()) {
            String tag = entry.getKey();
            Map<String, String> cfg = entry.getValue();
            boolean isDefault = DEFAULT_TAG.equals(tag);
            for (Map.Entry<String, String> w : cfg.entrySet()) {
                String key = w.getKey();
                if (key.startsWith("writer")) {
                    merged.put(isDefault ? key : renameWriterKey(key, tag), w.getValue());
                }
            }
            for (String writerName : collectWriterNames(cfg)) {
                merged.put((isDefault ? writerName : renameWriterName(writerName, tag)) + ".tag", tag);
            }
        }
        return merged;
    }

    private static Set<String> collectWriterNames(Map<String, String> cfg) {
        Set<String> names = new HashSet<>();
        for (String key : cfg.keySet()) {
            if (key.startsWith("writer")) {
                int dot = key.indexOf('.');
                names.add(dot == -1 ? key : key.substring(0, dot));
            }
        }
        return names;
    }

    private static String renameWriterKey(String key, String tag) {
        int dot = key.indexOf('.');
        String writerName = dot == -1 ? key : key.substring(0, dot);
        String property = dot == -1 ? "" : key.substring(dot);
        return renameWriterName(writerName, tag) + property;
    }

    /**
     * writer 名重命名：writer_file -> writer_<tag>_file，确保以 writer 开头且不冲突
     */
    private static String renameWriterName(String writerName, String tag) {
        String suffix = writerName.substring("writer".length());
        if (suffix.startsWith("_")) suffix = suffix.substring(1);
        return suffix.isEmpty() ? "writer_" + tag : "writer_" + tag + "_" + suffix;
    }

    @Deactivate
    public void shutdown() {
        if (showInfo != null) {
            showInfo.deactivate();
        }

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
        return org.tinylog.Logger.tag(tag).isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        org.tinylog.Logger.tag(tag).debug(msg);
    }

    @Override
    public void debug(String msg, Throwable t) {
        org.tinylog.Logger.tag(tag).debug(t, msg);
    }

    @Override
    public boolean isInfoEnabled() {
        return org.tinylog.Logger.tag(tag).isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        org.tinylog.Logger.tag(tag).info(msg);
    }

    @Override
    public void info(String msg, Throwable t) {
        org.tinylog.Logger.tag(tag).info(t, msg);
    }

    @Override
    public boolean isWarnEnabled() {
        return org.tinylog.Logger.tag(tag).isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        long suppressed = throttle.acquire('W', msg);
        if (suppressed >= 0) org.tinylog.Logger.tag(tag).warn(throttle.withSuppressed(msg, suppressed));
    }

    @Override
    public void warn(String msg, Throwable t) {
        long suppressed = throttle.acquire('W', msg);
        if (suppressed >= 0) org.tinylog.Logger.tag(tag).warn(t, throttle.withSuppressed(msg, suppressed));
    }

    @Override
    public boolean isErrorEnabled() {
        return org.tinylog.Logger.tag(tag).isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        long suppressed = throttle.acquire('E', msg);
        if (suppressed >= 0) org.tinylog.Logger.tag(tag).error(throttle.withSuppressed(msg, suppressed));
    }

    @Override
    public void error(String msg, Throwable t) {
        long suppressed = throttle.acquire('E', msg);
        if (suppressed >= 0) org.tinylog.Logger.tag(tag).error(t, throttle.withSuppressed(msg, suppressed));
    }
}
