package qingzhou.app.driver;

import qingzhou.logger.Logger;

public class AppLogger implements Logger {
    private final String app;
    private final Logger delegate;

    public AppLogger(String app, Logger delegate) {
        this.app = app;
        this.delegate = delegate;
    }

    @Override
    public boolean isDebugEnabled() {
        return delegate.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        delegate.debug("[" + app + "] " + msg);
    }

    @Override
    public void debug(String msg, Throwable t) {
        delegate.debug("[" + app + "] " + msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return delegate.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        delegate.info("[" + app + "] " + msg);
    }

    @Override
    public void info(String msg, Throwable t) {
        delegate.info("[" + app + "] " + msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return delegate.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        delegate.warn("[" + app + "] " + msg);
    }

    @Override
    public void warn(String msg, Throwable t) {
        delegate.warn("[" + app + "] " + msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return delegate.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        delegate.error("[" + app + "] " + msg);
    }

    @Override
    public void error(String msg, Throwable t) {
        delegate.error("[" + app + "] " + msg, t);
    }
}
