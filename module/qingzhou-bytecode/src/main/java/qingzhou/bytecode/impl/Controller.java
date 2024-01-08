package qingzhou.bytecode.impl;


import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import qingzhou.bytecode.BytecodeService;
import qingzhou.bytecode.impl.java.JavaServiceImpl;

public class Controller implements BundleActivator {
    private ServiceRegistration<BytecodeService> registerService;

    @Override
    public void start(BundleContext context) {
        registerService = context.registerService(BytecodeService.class, new JavaServiceImpl(), null);
    }

    @Override
    public void stop(BundleContext context) {
        registerService.unregister();
    }
}
