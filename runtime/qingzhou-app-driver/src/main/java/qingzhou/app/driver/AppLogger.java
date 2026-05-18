package qingzhou.app.driver;

import qingzhou.logger.Logger;
import qingzhou.logger.LoggerDecorator;

class AppLogger extends LoggerDecorator {
    private final String app;

    AppLogger(String app, Logger delegate) {
        super(delegate);
        this.app = app;
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
    public void info(String msg) {
        delegate.info("[" + app + "] " + msg);
    }

    @Override
    public void info(String msg, Throwable t) {
        delegate.info("[" + app + "] " + msg, t);
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
    public void error(String msg) {
        delegate.error("[" + app + "] " + msg);
    }

    @Override
    public void error(String msg, Throwable t) {
        delegate.error("[" + app + "] " + msg, t);
    }
}
