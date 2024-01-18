package qingzhou.console.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import qingzhou.console.servlet.ServletService;
import qingzhou.console.servlet.impl.ServletImpl;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.pattern.Process;
import qingzhou.framework.pattern.ProcessSequence;
import qingzhou.framework.util.FileUtil;

import java.io.File;

public class Controller implements BundleActivator {
    private ProcessSequence sequence;
    FrameworkContext frameworkContext;
    ServletService servletService;

    @Override
    public void start(BundleContext context) throws Exception {
        sequence = new ProcessSequence(
                new InitConsoleEnv(context),
                new StartServlet(),
                new RunWar()
        );
        sequence.exec();
    }

    @Override
    public void stop(BundleContext context) {
        sequence.undo();
    }

    private class RunWar implements Process {
        private String contextPath;

        @Override
        public void exec() {
            if (!frameworkContext.isMaster()) return;

            File console = FileUtil.newFile(frameworkContext.getLib(), "sysapp", "console");
            String docBase = console.getAbsolutePath();
            contextPath = "/console"; // TODO 需要可配置
            servletService.addWebapp(contextPath, docBase);
            ConsoleWarHelper.getLogger().info("Open a browser to access the QingZhou console: http://localhost:9060" + contextPath);// todo 9060 应该动态获取到
        }

        @Override
        public void undo() {
            if (!frameworkContext.isMaster()) return;

            servletService.removeApp(contextPath);
        }
    }

    private class StartServlet implements Process {
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

    private class InitConsoleEnv implements Process {
        private final BundleContext context;
        private ServiceReference<FrameworkContext> reference;

        private InitConsoleEnv(BundleContext context) {
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
