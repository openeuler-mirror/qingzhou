package qingzhou.serializer.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import qingzhou.serializer.SerializerService;
import qingzhou.serializer.impl.java.JavaSerializer;

public class Controller implements BundleActivator {
    private ServiceRegistration<SerializerService> registration;

    @Override
    public void start(BundleContext context) {
        registration = context.registerService(SerializerService.class, JavaSerializer::new, null);
    }

    @Override
    public void stop(BundleContext context) {
        registration.unregister();
    }
}
