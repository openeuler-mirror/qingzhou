package qingzhou.serializer.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import qingzhou.serializer.Serializer;
import qingzhou.serializer.impl.java.JavaSerializer;

public class Controller implements BundleActivator {
    private ServiceRegistration<Serializer> registration;

    @Override
    public void start(BundleContext context) {
        registration = context.registerService(Serializer.class, new JavaSerializer(), null);
    }

    @Override
    public void stop(BundleContext context) {
        registration.unregister();
    }
}
