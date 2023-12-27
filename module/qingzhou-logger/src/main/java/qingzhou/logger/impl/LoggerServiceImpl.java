package qingzhou.logger.impl;

import qingzhou.logger.Logger;
import qingzhou.logger.LoggerService;

public class LoggerServiceImpl implements LoggerService {
    private final Logger defaultLogger = new LoggerImpl();

    @Override
    public Logger getLogger() {
        return defaultLogger;
    }

    private static class LoggerImpl implements Logger {

        @Override
        public void debug(String msg) {
            org.tinylog.Logger.debug(msg);
        }

        @Override
        public void debug(String msg, Throwable t) {
            org.tinylog.Logger.debug(t, msg);
        }

        @Override
        public void info(String msg) {
            org.tinylog.Logger.info(msg);
        }

        @Override
        public void info(String msg, Throwable t) {
            org.tinylog.Logger.info(t, msg);
        }

        @Override
        public void warn(String msg) {
            org.tinylog.Logger.warn(msg);
        }

        @Override
        public void warn(String msg, Throwable t) {
            org.tinylog.Logger.warn(t, msg);
        }

        @Override
        public void error(String msg) {
            org.tinylog.Logger.error(msg);
        }

        @Override
        public void error(String msg, Throwable t) {
            org.tinylog.Logger.error(t, msg);
        }
    }

    private static class NotSupportedException extends RuntimeException {
    }
}
