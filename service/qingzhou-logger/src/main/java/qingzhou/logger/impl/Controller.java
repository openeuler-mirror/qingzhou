package qingzhou.logger.impl;

import qingzhou.framework.service.ServiceRegister;
import qingzhou.framework.api.Logger;

public class Controller extends ServiceRegister<Logger> {
    private final LoggerImpl defaultLogger = new LoggerImpl();

    @Override
    protected Class<Logger> serviceType() {
        return Logger.class;
    }

    @Override
    protected Logger serviceObject() {
        return defaultLogger;
    }
}
