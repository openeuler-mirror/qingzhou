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

public class Controller implements BundleActivator {
    private ProcessSequence processSequence;
    private FrameworkContextImpl frameworkContext;

    @Override
    public void start(BundleContext context) throws Exception {
        processSequence = new ProcessSequence(
                new InitFramework(context),
                new InitLogger(context),
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

    private class InitLogger implements Process {
        private final BundleContext context;
        private ServiceReference<LoggerService> reference;

        private InitLogger(BundleContext context) {
            this.context = context;
        }

        @Override
        public void exec() {
            reference = context.getServiceReference(LoggerService.class);
            LoggerService loggerService = context.getService(reference);
            frameworkContext.registerService(LoggerService.class, loggerService);
        }

        @Override
        public void undo() {
            context.ungetService(reference);
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

            FrameworkContextImpl.frameworkContext = frameworkContext;
        }

        @Override
        public void undo() {
            registration.unregister();
            frameworkContext = null;
        }
    }
}
