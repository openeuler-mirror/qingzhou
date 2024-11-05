package qingzhou.logger;

import qingzhou.engine.ServiceInfo;

public interface Logger extends ServiceInfo {
    @Override
    default String getDescription() {
        return "Provide practical tools related to Logger.";
    }

    boolean isDebugEnabled();

    void debug(String msg);

    void debug(String msg, Throwable t);

    boolean isInfoEnabled();

    void info(String msg);

    void info(String msg, Throwable t);

    boolean isWarnEnabled();

    void warn(String msg);

    void warn(String msg, Throwable t);

    boolean isErrorEnabled();

    void error(String msg);

    void error(String msg, Throwable t);
}
