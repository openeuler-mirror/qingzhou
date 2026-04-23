package qingzhou.logger.impl;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import qingzhou.logger.Logger;

@Component(configurationPid = "qingzhou-logger", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class LoggerImpl implements Logger {
    private ShowInfo showInfo;

    @Activate
    public void init(Map<String, Object> config) {
        Map<String, String> loggerConfig = new HashMap<String, String>() {{
            put("autoshutdown", "false"); // 必要的默认配置
        }};

        config.entrySet().stream().filter(e -> e.getValue() instanceof String)
                .forEach(e -> loggerConfig.put(e.getKey(), String.valueOf(e.getValue())));

        org.tinylog.configuration.Configuration.replace(loggerConfig);

        showInfo = new ShowInfo(this);
        showInfo.activate();
    }

    @Deactivate
    public void shutdown() {
        showInfo.deactivate();

        try {
            org.tinylog.provider.ProviderRegistry.getLoggingProvider().shutdown();
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return org.tinylog.Logger.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        org.tinylog.Logger.debug(msg);
    }

    @Override
    public void debug(String msg, Throwable t) {
        org.tinylog.Logger.debug(t, msg);
    }

    @Override
    public boolean isInfoEnabled() {
        return org.tinylog.Logger.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        org.tinylog.Logger.info(msg);
    }

    @Override
    public void info(String msg, Throwable t) {
        org.tinylog.Logger.info(t, msg);
    }

    @Override
    public boolean isWarnEnabled() {
        return org.tinylog.Logger.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        org.tinylog.Logger.warn(msg);
    }

    @Override
    public void warn(String msg, Throwable t) {
        org.tinylog.Logger.warn(t, msg);
    }

    @Override
    public boolean isErrorEnabled() {
        return org.tinylog.Logger.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        org.tinylog.Logger.error(msg);
    }

    @Override
    public void error(String msg, Throwable t) {
        org.tinylog.Logger.error(t, msg);
    }
}
