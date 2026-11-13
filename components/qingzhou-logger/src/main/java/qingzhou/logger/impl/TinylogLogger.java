package qingzhou.logger.impl;

import java.util.function.Consumer;

import qingzhou.logger.Logger;

/**
 * 单个命名日志器：绑定 tinylog tag 与独立限流窗口，避免不同日志器的限流互相干扰。
 */
class TinylogLogger implements Logger {
    private final org.tinylog.TaggedLogger tinylog;
    private final LogThrottle throttle;

    TinylogLogger(String name, LogThrottle throttle) {
        this.tinylog = org.tinylog.Logger.tag(name);
        this.throttle = throttle;
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