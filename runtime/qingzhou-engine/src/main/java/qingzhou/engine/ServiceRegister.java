package qingzhou.engine;

public abstract class ServiceRegister<T> implements Module {
    protected ModuleContext moduleContext;
    private RegistryKey registryKey;

    @Override
    public final void start(ModuleContext moduleContext) throws Exception {
        this.moduleContext = moduleContext;
        startService();
        registryKey = moduleContext.registerService(serviceType(), serviceObject());
    }

    @Override
    public final void stop() {
        moduleContext.unregisterService(registryKey);
        stopService();
    }

    public abstract Class<T> serviceType();

    protected abstract T serviceObject();

    protected void startService() throws Exception {
    }

    protected void stopService() {
    }
}
