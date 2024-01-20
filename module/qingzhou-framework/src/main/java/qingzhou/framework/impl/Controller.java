package qingzhou.framework.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.pattern.Process;
import qingzhou.framework.pattern.ProcessSequence;
import qingzhou.logger.Logger;
import qingzhou.logger.LoggerService;

import java.util.HashSet;
import java.util.Set;

public class Controller implements BundleActivator {
    private ProcessSequence processSequence;
    private FrameworkContextImpl frameworkContext;

    @Override
    public void start(BundleContext context) throws Exception {
        processSequence = new ProcessSequence(
                new InitFramework(context),
                new InitBasicServices(context),
                new ShowInfo()
        );
        processSequence.exec();
    }

    @Override
    public void stop(BundleContext context) {
        processSequence.undo();
    }

    private class ShowInfo implements Process {
        private final String[] BANNER = {"",
                "                 |~",
                "           |/    w",
                "          / (   (|   \\",
                "         /( (/   |)  |\\",
                "  ____  ( (/    (|   | )  ,",
                " |----\\ (/ |    /|   |'\\ /^;",
                "\\---*---Y--+-----+---+--/(",
                " \\------*---*--*---*--/",
                "  '~~ ~~~~~~~~~~~~~~~"};

        @Override
        public void exec() {
            Logger logger = frameworkContext.getService(LoggerService.class).getLogger();

            for (String line : BANNER) {
                logger.info(line);
            }
            // 打印版本信息
            logger.info("");
            logger.info("    Powered by QingZhou");
            logger.info("");
        }
    }

    private class InitBasicServices implements Process {
        private final BundleContext context;
        private final Set<ServiceReference<?>> serviceReferences = new HashSet<>();

        private InitBasicServices(BundleContext context) {
            this.context = context;
        }

        @Override
        public void exec() {
            Class<?>[] serviceTypes = {LoggerService.class};
            for (Class serviceType : serviceTypes) {
                ServiceReference<?> reference = context.getServiceReference(serviceType);
                Object serviceObj = context.getService(reference);
                frameworkContext.registerService(serviceType, serviceObj);
                serviceReferences.add(reference);
            }
        }

        @Override
        public void undo() {
            serviceReferences.forEach(context::ungetService);
        }
    }

    private class InitFramework implements Process {
        private final BundleContext context;
        private ServiceRegistration<FrameworkContext> registration;

        private InitFramework(BundleContext context) {
            this.context = context;
        }

        @Override
        public void exec() {
            frameworkContext = new FrameworkContextImpl();
            registration = context.registerService(FrameworkContext.class, frameworkContext, null);
        }

        @Override
        public void undo() {
            registration.unregister();
            frameworkContext = null;
        }
    }
}
