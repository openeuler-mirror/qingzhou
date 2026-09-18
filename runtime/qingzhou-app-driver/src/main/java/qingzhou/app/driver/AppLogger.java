package qingzhou.app.driver;

import qingzhou.logger.Logger;
import qingzhou.logger.LoggerDecorator;

class AppLogger extends LoggerDecorator {
    private final String app;

    AppLogger(String app, Logger delegate) {
        super(delegate);
        this.app = app;
    }

    private String wrap(String msg) {
        return "[" + app + "] " + msg;
    }

    @Override
    public void debug(String msg) {
        delegate.debug(wrap(msg));
    }

    @Override
    public void debug(String msg, Throwable t) {
        delegate.debug(wrap(msg), t);
    }

    @Override
    public void info(String msg) {
        delegate.info(wrap(msg));
    }

    @Override
    public void info(String msg, Throwable t) {
        delegate.info(wrap(msg), t);
    }

    @Override
    public void warn(String msg) {
        delegate.warn(wrap(msg));
    }

    @Override
    public void warn(String msg, Throwable t) {
        delegate.warn(wrap(msg), t);
    }

    @Override
    public void error(String msg) {
        delegate.error(wrap(msg));
    }

    @Override
    public void error(String msg, Throwable t) {
        delegate.error(wrap(msg), t);
    }
}
