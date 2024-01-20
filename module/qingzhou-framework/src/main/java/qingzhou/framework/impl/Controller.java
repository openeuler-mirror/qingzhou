package qingzhou.framework.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.Logger;

public class Controller implements BundleActivator {
    private ServiceRegistration<FrameworkContext> registration;

    @Override
    public void start(BundleContext context) throws Exception {
        FrameworkContextImpl frameworkContext = new FrameworkContextImpl();
        registration = context.registerService(FrameworkContext.class, frameworkContext, null);

        frameworkContext.addServiceListener(serviceType -> {
            if (serviceType == Logger.class) {
                Logger logger = frameworkContext.getService(Logger.class);
                frameworkContext.setLogger(logger);
                Controller.this.showInfo(logger);
            }
        });
    }

    @Override
    public void stop(BundleContext context) {
        registration.unregister();
    }

    private void showInfo(Logger logger) {
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
            logger.info(line);
        }
    }
}
