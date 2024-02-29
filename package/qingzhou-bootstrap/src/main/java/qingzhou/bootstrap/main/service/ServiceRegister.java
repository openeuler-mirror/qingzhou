package qingzhou.bootstrap.main.service;

import qingzhou.bootstrap.main.FrameworkContext;
import qingzhou.bootstrap.main.ModuleLoader;

public abstract class ServiceRegister<T> implements ModuleLoader {
    protected FrameworkContext frameworkContext;
    private RegistryKey registryKey;

    @Override
    public final void start(FrameworkContext frameworkContext) throws Exception {
        this.frameworkContext = frameworkContext;
        startService();
        registryKey = frameworkContext.getServiceManager().registerService(serviceType(), serviceObject());
    }

    @Override
    public final void stop(FrameworkContext frameworkContext) {
        frameworkContext.getServiceManager().unregisterService(registryKey);
        stopService();
    }

    public abstract Class<T> serviceType();

    protected abstract T serviceObject();

    protected void startService() throws Exception {
    }

    protected void stopService() {
    }
}
