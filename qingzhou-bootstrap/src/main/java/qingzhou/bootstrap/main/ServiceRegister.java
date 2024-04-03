package qingzhou.bootstrap.main;

public abstract class ServiceRegister<T> implements Module {
    protected FrameworkContext frameworkContext;
    private RegistryKey registryKey;

    @Override
    public final void start(FrameworkContext frameworkContext) throws Exception {
        this.frameworkContext = frameworkContext;
        startService();
        registryKey = frameworkContext.registerService(serviceType(), serviceObject());
    }

    @Override
    public final void stop() {
        frameworkContext.unregisterService(registryKey);
        stopService();
    }

    public abstract Class<T> serviceType();

    protected abstract T serviceObject();

    protected void startService() throws Exception {
    }

    protected void stopService() {
    }
}
