package qingzhou.console;

import org.apache.juli.logging.Log;
import qingzhou.console.impl.ConsoleWarHelper;

public class LogDelegate implements Log {
    public LogDelegate() {
    }

    public LogDelegate(String loggerName) {
    }

    @Override
    public boolean isDebugEnabled() {
        return ConsoleWarHelper.getLogger().isDebugEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return ConsoleWarHelper.getLogger().isErrorEnabled();
    }

    @Override
    public boolean isFatalEnabled() {
        return ConsoleWarHelper.getLogger().isErrorEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return ConsoleWarHelper.getLogger().isInfoEnabled();
    }

    @Override
    public boolean isTraceEnabled() {
        return ConsoleWarHelper.getLogger().isDebugEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return ConsoleWarHelper.getLogger().isWarnEnabled();
    }

    @Override
    public void trace(Object message) {
        ConsoleWarHelper.getLogger().debug(String.valueOf(message));
    }

    @Override
    public void trace(Object message, Throwable t) {
        ConsoleWarHelper.getLogger().debug(String.valueOf(message), t);
    }

    @Override
    public void debug(Object message) {
        ConsoleWarHelper.getLogger().debug(String.valueOf(message));
    }

    @Override
    public void debug(Object message, Throwable t) {
        ConsoleWarHelper.getLogger().debug(String.valueOf(message), t);
    }

    @Override
    public void info(Object message) {
        ConsoleWarHelper.getLogger().info(String.valueOf(message));
    }

    @Override
    public void info(Object message, Throwable t) {
        ConsoleWarHelper.getLogger().info(String.valueOf(message), t);
    }

    @Override
    public void warn(Object message) {
        ConsoleWarHelper.getLogger().warn(String.valueOf(message));
    }

    @Override
    public void warn(Object message, Throwable t) {
        ConsoleWarHelper.getLogger().warn(String.valueOf(message), t);
    }

    @Override
    public void error(Object message) {
        ConsoleWarHelper.getLogger().error(String.valueOf(message));
    }

    @Override
    public void error(Object message, Throwable t) {
        ConsoleWarHelper.getLogger().error(String.valueOf(message), t);
    }

    @Override
    public void fatal(Object message) {
        ConsoleWarHelper.getLogger().error(String.valueOf(message));
    }

    @Override
    public void fatal(Object message, Throwable t) {
        ConsoleWarHelper.getLogger().error(String.valueOf(message), t);
    }
}
