package qingzhou.core.console.servlet.impl;

import org.apache.juli.logging.Log;
import qingzhou.impl.Controller;

public class TomcatLogDelegate implements Log {
    public TomcatLogDelegate() {
    }

    public TomcatLogDelegate(String loggerName) {
    }

    @Override
    public boolean isDebugEnabled() {
        return Controller.logger.isDebugEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return Controller.logger.isErrorEnabled();
    }

    @Override
    public boolean isFatalEnabled() {
        return Controller.logger.isErrorEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return Controller.logger.isInfoEnabled();
    }

    @Override
    public boolean isTraceEnabled() {
        return Controller.logger.isDebugEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return Controller.logger.isWarnEnabled();
    }

    @Override
    public void trace(Object message) {
        Controller.logger.debug(String.valueOf(message));
    }

    @Override
    public void trace(Object message, Throwable t) {
        Controller.logger.debug(String.valueOf(message), t);
    }

    @Override
    public void debug(Object message) {
        Controller.logger.debug(String.valueOf(message));
    }

    @Override
    public void debug(Object message, Throwable t) {
        Controller.logger.debug(String.valueOf(message), t);
    }

    @Override
    public void info(Object message) {
        Controller.logger.info(String.valueOf(message));
    }

    @Override
    public void info(Object message, Throwable t) {
        Controller.logger.info(String.valueOf(message), t);
    }

    @Override
    public void warn(Object message) {
        Controller.logger.warn(String.valueOf(message));
    }

    @Override
    public void warn(Object message, Throwable t) {
        Controller.logger.warn(String.valueOf(message), t);
    }

    @Override
    public void error(Object message) {
        Controller.logger.error(String.valueOf(message));
    }

    @Override
    public void error(Object message, Throwable t) {
        Controller.logger.error(String.valueOf(message), t);
    }

    @Override
    public void fatal(Object message) {
        Controller.logger.error(String.valueOf(message));
    }

    @Override
    public void fatal(Object message, Throwable t) {
        Controller.logger.error(String.valueOf(message), t);
    }
}
