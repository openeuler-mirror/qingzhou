package qingzhou.logger.impl;

import org.slf4j.helpers.MessageFormatter;
import org.slf4j.helpers.MarkerIgnoringBase;

public class Slf4jLogger extends MarkerIgnoringBase {
    private static final long serialVersionUID = 1L;

    Slf4jLogger(String name) {
        this.name = name;
    }

    private qingzhou.logger.Logger delegate() {
        return Slf4jBridge.getQingzhouLogger();
    }

    @Override
    public boolean isTraceEnabled() {
        qingzhou.logger.Logger log = delegate();
        return log != null && log.isDebugEnabled();
    }

    @Override
    public void trace(String msg) {
        qingzhou.logger.Logger log = delegate();
        if (log != null) log.debug(format(msg));
    }

    @Override
    public void trace(String format, Object arg) {
        qingzhou.logger.Logger log = delegate();
        if (log != null) log.debug(format(format, arg));
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        qingzhou.logger.Logger log = delegate();
        if (log != null) log.debug(format(format, arg1, arg2));
    }

    @Override
    public void trace(String format, Object... arguments) {
        qingzhou.logger.Logger log = delegate();
        if (log != null) log.debug(format(format, arguments));
    }

    @Override
    public void trace(String msg, Throwable t) {
        qingzhou.logger.Logger log = delegate();
        if (log != null) log.debug(format(msg), t);
    }

    @Override
    public boolean isDebugEnabled() {
        qingzhou.logger.Logger log = delegate();
        return log != null && log.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        qingzhou.logger.Logger log = delegate();
        if (log != null) log.debug(format(msg));
    }

    @Override
    public void debug(String format, Object arg) {
        qingzhou.logger.Logger log = delegate();
        if (log != null) log.debug(format(format, arg));
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        qingzhou.logger.Logger log = delegate();
        if (log != null) log.debug(format(format, arg1, arg2));
    }

    @Override
    public void debug(String format, Object... arguments) {
        qingzhou.logger.Logger log = delegate();
        if (log != null) log.debug(format(format, arguments));
    }

    @Override
    public void debug(String msg, Throwable t) {
        qingzhou.logger.Logger log = delegate();
        if (log != null) log.debug(format(msg), t);
    }

    @Override
    public boolean isInfoEnabled() {
        qingzhou.logger.Logger log = delegate();
        return log != null && log.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        qingzhou.logger.Logger log = delegate();
        if (log != null) log.info(format(msg));
    }

    @Override
    public void info(String format, Object arg) {
        qingzhou.logger.Logger log = delegate();
        if (log != null) log.info(format(format, arg));
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        qingzhou.logger.Logger log = delegate();
        if (log != null) log.info(format(format, arg1, arg2));
    }

    @Override
    public void info(String format, Object... arguments) {
        qingzhou.logger.Logger log = delegate();
        if (log != null) log.info(format(format, arguments));
    }

    @Override
    public void info(String msg, Throwable t) {
        qingzhou.logger.Logger log = delegate();
        if (log != null) log.info(format(msg), t);
    }

    @Override
    public boolean isWarnEnabled() {
        qingzhou.logger.Logger log = delegate();
        return log != null && log.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        qingzhou.logger.Logger log = delegate();
        if (log != null) log.warn(format(msg));
    }

    @Override
    public void warn(String format, Object arg) {
        qingzhou.logger.Logger log = delegate();
        if (log != null) log.warn(format(format, arg));
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        qingzhou.logger.Logger log = delegate();
        if (log != null) log.warn(format(format, arg1, arg2));
    }

    @Override
    public void warn(String format, Object... arguments) {
        qingzhou.logger.Logger log = delegate();
        if (log != null) log.warn(format(format, arguments));
    }

    @Override
    public void warn(String msg, Throwable t) {
        qingzhou.logger.Logger log = delegate();
        if (log != null) log.warn(format(msg), t);
    }

    @Override
    public boolean isErrorEnabled() {
        qingzhou.logger.Logger log = delegate();
        return log != null && log.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        qingzhou.logger.Logger log = delegate();
        if (log != null) log.error(format(msg));
    }

    @Override
    public void error(String format, Object arg) {
        qingzhou.logger.Logger log = delegate();
        if (log != null) log.error(format(format, arg));
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        qingzhou.logger.Logger log = delegate();
        if (log != null) log.error(format(format, arg1, arg2));
    }

    @Override
    public void error(String format, Object... arguments) {
        qingzhou.logger.Logger log = delegate();
        if (log != null) log.error(format(format, arguments));
    }

    @Override
    public void error(String msg, Throwable t) {
        qingzhou.logger.Logger log = delegate();
        if (log != null) log.error(format(msg), t);
    }

    private String format(String msg) {
        return "[SLF4J][" + name + "] " + msg;
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
}
