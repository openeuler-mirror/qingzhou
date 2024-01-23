package qingzhou.framework.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.ServiceListener;
import qingzhou.framework.api.Logger;

public class Controller implements BundleActivator {
    private ServiceRegistration<FrameworkContext> registration;
    private FrameworkContextImpl frameworkContext;

    @Override
    public void start(BundleContext context) throws Exception {
        frameworkContext = new FrameworkContextImpl();
        registration = context.registerService(FrameworkContext.class, frameworkContext, null);

        frameworkContext.getServiceManager().addServiceListener(new ServiceListener() {
            @Override
            public void serviceRegistered(Class<?> serviceType) {
                if (serviceType == Logger.class) {
                    Logger logger = frameworkContext.getServiceManager().getService(Logger.class);
                    frameworkContext.setLogger(logger);

                    startInfo();
                }
            }

            @Override
            public void serviceUnregistered(Class<?> serviceType, Object serviceObj) {
                if (serviceType == Logger.class) {
                    stopInfo();
                    frameworkContext.setLogger(null);
                }
            }
        });
    }

    @Override
    public void stop(BundleContext context) {
        registration.unregister();
        frameworkContext = null;
    }

    private void stopInfo() {

    }

    private void startInfo() {
        String[] banner = {"",
                " Qing Zhou       |~",
                "             |/  w",
                "             / ((| \\",
                "            /((/ |)|\\",
                "  ____     ((/  (| | )  ,",
                " |----\\   (/ |  /| |'\\ /^;",
                "\\---*---Y--+-----+---+--/(",
                " \\------*---*--*---*--/",
                "  '~~ ~~~~~~~~~~~~~~~",
                ""};
        for (String line : banner) {
            frameworkContext.getLogger().info(line);
        }
    }
}
