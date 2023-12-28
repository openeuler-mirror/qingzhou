package qingzhou.console.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import qingzhou.console.servlet.ServletService;
import qingzhou.console.servlet.impl.ServletImpl;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.pattern.Process;
import qingzhou.framework.pattern.ProcessSequence;

public class Controller implements BundleActivator {
    private ProcessSequence sequence;
    FrameworkContext frameworkContext;
    ServletService servletService;

    @Override
    public void start(BundleContext context) throws Exception {
        sequence = new ProcessSequence(
                new GetFrameworkService(context),
                new StartServlet(),
                new InitMasterApp(frameworkContext),// todo frameworkContext 应该没有初始化
                new RunWar(this),
                new RunRemote(this)
        );
        sequence.exec();
    }

    @Override
    public void stop(BundleContext context) {
        sequence.undo();
    }

    private class StartServlet implements Process {

        private StartServlet() {
        }

        @Override
        public void exec() throws Exception {
            servletService = new ServletImpl();
            servletService.start(9060, // TODO 端口需要可以配置
                    frameworkContext.getCache().getAbsolutePath());
        }

        @Override
        public void undo() {
            servletService.stop();
        }
    }

    private class GetFrameworkService implements Process {
        private final BundleContext context;
        private ServiceReference<FrameworkContext> reference;

        private GetFrameworkService(BundleContext context) {
            this.context = context;
        }

        @Override
        public void exec() {
            reference = context.getServiceReference(FrameworkContext.class);
            frameworkContext = context.getService(this.reference);
            ConsoleWarHelper.fc = frameworkContext;
        }

        @Override
        public void undo() {
            frameworkContext = null;
            context.ungetService(reference);
        }
    }
}
