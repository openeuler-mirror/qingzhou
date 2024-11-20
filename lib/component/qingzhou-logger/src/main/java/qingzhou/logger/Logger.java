package qingzhou.logger;

import qingzhou.engine.Service;

@Service(name = "Easy Logger", description = "A simple and easy-to-use log system implementation can unify the logs of applications to the Qingzhou platform.")
public interface Logger {
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

    void shutdown();
}
