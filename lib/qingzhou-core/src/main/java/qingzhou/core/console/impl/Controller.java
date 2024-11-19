package qingzhou.core.console.impl;

import qingzhou.core.config.Config;
import qingzhou.core.config.Console;
import qingzhou.core.AppPageData;
import qingzhou.core.console.ContextHelper;
import qingzhou.core.console.servlet.ServletContainer;
import qingzhou.core.console.servlet.impl.ServletContainerImpl;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.pattern.Process;
import qingzhou.engine.util.pattern.ProcessSequence;
import qingzhou.logger.Logger;

import java.io.File;
import java.util.Properties;

public class Controller implements Process {
    public final ModuleContext moduleContext;
    private Console console;
    private ProcessSequence sequence;
    private ServletContainer servletContainer;

    public Controller(ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }

    @Override
    public void exec() throws Exception {
        console = moduleContext.getService(Config.class).getCore().getConsole();

        if (console == null || !console.isEnabled()) return;

        sequence = new ProcessSequence(
                new StartServletContainer(),
                new DeployWar()
        );
        sequence.exec();
    }

    @Override
    public void undo() {
        if (sequence != null) {
            sequence.undo();
        }
    }

    private class StartServletContainer implements Process {
        @Override
        public void exec() throws Exception {
            servletContainer = new ServletContainerImpl();
            servletContainer.start(console.getWeb().getPort(),
                    new File(moduleContext.getTemp(), "servlet"),
                    new Properties() {{
                        setProperty("maxPostSize", String.valueOf(console.getWeb().getMaxPostSize()));
                    }});
        }

        @Override
        public void undo() {
            servletContainer.stop();
        }
    }

    private class DeployWar implements Process {
        private String contextPath;

        @Override
        public void exec() {
            try {
                ContextHelper.GET_INSTANCE.set(() -> Controller.this.moduleContext);
                exec0();
            } finally {
                ContextHelper.GET_INSTANCE.remove();
            }
        }

        private void exec0() {
            File consoleApp = FileUtil.newFile(moduleContext.getLibDir(), "module", "console");
            String docBase = consoleApp.getAbsolutePath();
            contextPath = console.getWeb().getContextRoot();
            servletContainer.addWebapp(contextPath, docBase, new Properties() {{
                setProperty("webResources", "/=" + FileUtil.newFile(moduleContext.getTemp(), AppPageData.DOWNLOAD_PAGE_ROOT_DIR).getAbsoluteFile());
            }});
            moduleContext.getService(Logger.class).info("Open a browser to access the Qingzhou console: http://localhost:" + console.getWeb().getPort() + contextPath);
        }

        @Override
        public void undo() {
            servletContainer.removeApp(contextPath);
        }
    }
}
