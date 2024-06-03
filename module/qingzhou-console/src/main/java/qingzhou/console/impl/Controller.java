package qingzhou.console.impl;

import qingzhou.config.Config;
import qingzhou.config.Console;
import qingzhou.console.ContextHelper;
import qingzhou.deployer.Deployer;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.Service;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Process;
import qingzhou.engine.util.pattern.ProcessSequence;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.Registry;
import qingzhou.servlet.ServletContainer;
import qingzhou.servlet.ServletService;

import java.io.File;

@Module
public class Controller implements ModuleActivator {
    @Service
    private Logger logger;
    @Service
    private Config config;
    @Service
    private ServletService servletService;
    @Service
    private Deployer deployer;
    @Service
    private Registry registry;

    @Service
    private Json json;// RemoteClient会用到

    private Console console;
    private ProcessSequence sequence;
    private ServletContainer servletContainer;
    public ModuleContext moduleContext;
    private Controller instance;

    public Controller() {
        instance = this;
    }

    @Override
    public void start(ModuleContext context) throws Exception {
        moduleContext = context;
        console = config.getConsole();

        if (!console.isEnabled()) return;

        sequence = new ProcessSequence(
                new StartServletContainer(),
                new DeployWar()
        );
        sequence.exec();
    }

    @Override
    public void stop() {
        if (sequence != null) {
            sequence.undo();
        }
    }

    private class StartServletContainer implements Process {
        @Override
        public void exec() throws Exception {
            servletContainer = servletService.createServletContainer();
            servletContainer.start(console.getPort(),
                    new File(moduleContext.getTemp(), "servlet"));
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
                ContextHelper.GetInstance.set(new ContextHelper() {
                    @Override
                    public <T> T getService(Class<T> type) {
                        return moduleContext.getService(type);
                    }

                    @Override
                    public ModuleContext getModuleContext() {
                        return Controller.this.moduleContext;
                    }
                });

                exec0();
            } finally {
                ContextHelper.GetInstance.remove();
            }
        }

        private void exec0() {
            File consoleApp = Utils.newFile(moduleContext.getLibDir(), "module", "console");
            String docBase = consoleApp.getAbsolutePath();
            contextPath = console.getContextRoot();
            servletContainer.addWebapp(contextPath, docBase);
            logger.info("Open a browser to access the Qingzhou console: http://localhost:" + console.getPort() + contextPath);
        }

        @Override
        public void undo() {
            servletContainer.removeApp(contextPath);
        }
    }
}
