package qingzhou.llm.impl.log;

import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

class Slf4jLogger extends MarkerIgnoringBase {
    private final qingzhou.logger.Logger delegate;

    Slf4jLogger(String name, qingzhou.logger.Logger delegate) {
        this.name = name;
        this.delegate = delegate;
    }

    @Override
    public boolean isTraceEnabled() {
        return delegate.isDebugEnabled();
    }

    @Override
    public void trace(String msg) {
        delegate.debug(format(msg));
    }

    @Override
    public void trace(String format, Object arg) {
        delegate.debug(format(format, arg));
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        delegate.debug(format(format, arg1, arg2));
    }

    @Override
    public void trace(String format, Object... arguments) {
        delegate.debug(format(format, arguments));
    }

    @Override
    public void trace(String msg, Throwable t) {
        delegate.debug(format(msg), t);
    }

    @Override
    public boolean isDebugEnabled() {
        return delegate.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        delegate.debug(format(msg));
    }

    @Override
    public void debug(String format, Object arg) {
        delegate.debug(format(format, arg));
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        delegate.debug(format(format, arg1, arg2));
    }

    @Override
    public void debug(String format, Object... arguments) {
        delegate.debug(format(format, arguments));
    }

    @Override
    public void debug(String msg, Throwable t) {
        delegate.debug(format(msg), t);
    }

    @Override
    public boolean isInfoEnabled() {
        return delegate.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        delegate.info(format(msg));
    }

    @Override
    public void info(String format, Object arg) {
        delegate.info(format(format, arg));
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        delegate.info(format(format, arg1, arg2));
    }

    @Override
    public void info(String format, Object... arguments) {
        delegate.info(format(format, arguments));
    }

    @Override
    public void info(String msg, Throwable t) {
        delegate.info(format(msg), t);
    }

    @Override
    public boolean isWarnEnabled() {
        return delegate.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        delegate.warn(format(msg));
    }

    @Override
    public void warn(String format, Object arg) {
        delegate.warn(format(format, arg));
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        delegate.warn(format(format, arg1, arg2));
    }

    @Override
    public void warn(String format, Object... arguments) {
        delegate.warn(format(format, arguments));
    }

    @Override
    public void warn(String msg, Throwable t) {
        delegate.warn(format(msg), t);
    }

    @Override
    public boolean isErrorEnabled() {
        return delegate.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        delegate.error(format(msg));
    }

    @Override
    public void error(String format, Object arg) {
        delegate.error(format(format, arg));
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        delegate.error(format(format, arg1, arg2));
    }

    @Override
    public void error(String format, Object... arguments) {
        delegate.error(format(format, arguments));
    }

    @Override
    public void error(String msg, Throwable t) {
        delegate.error(format(msg), t);
    }

    private String format(String format, Object arg) {
        return format(MessageFormatter.format(format, arg).getMessage());
    }

    private String format(String format, Object arg1, Object arg2) {
        return format(MessageFormatter.format(format, arg1, arg2).getMessage());
    }

    private String format(String format, Object... args) {
        return format(MessageFormatter.arrayFormat(format, args).getMessage());
    }

    private String format(String msg) {
        return "[SOLON] [" + name + "] " + msg;
    }
}
