package qingzhou.logger.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import qingzhou.logger.LoggerService;
import qingzhou.logger.SystemPrintApiBackup;

public class Controller implements BundleActivator {
    private ServiceRegistration<LoggerService> registerService;

    @Override
    public void start(BundleContext context) {
        if (SystemPrintApiBackup.out == null) {
            // 触发 ConsolePrintStream.out 的加载；
            throw new IllegalStateException();
        }
        if (SystemPrintApiBackup.err == null) {
            // 触发 ConsolePrintStream.err 的加载；
            throw new IllegalStateException();
        }

        registerService = context.registerService(LoggerService.class, new LoggerServiceImpl(), null);
    }

    @Override
    public void stop(BundleContext context) {
        registerService.unregister();
    }
}
