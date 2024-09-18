package qingzhou.console.impl;

import qingzhou.config.Config;
import qingzhou.config.Console;
import qingzhou.console.ContextHelper;
import qingzhou.crypto.CryptoService;
import qingzhou.deployer.ActionInvoker;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.JmxServiceAdapter;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.Service;
import qingzhou.engine.util.FileUtil;
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
    private CryptoService cryptoService;
    @Service
    private Json json;// RemoteClient会用到
    @Service(optional = true) // console 禁用（远程受管实例）后，此对象为 null，optional = true
    private JmxServiceAdapter jmxServiceAdapter;
    @Service
    private ActionInvoker actionInvoker;

    private Console console;
    private ProcessSequence sequence;
    private ServletContainer servletContainer;
    public ModuleContext moduleContext;

    public Controller() {
    }

    @Override
    public void start(ModuleContext context) throws Exception {
        moduleContext = context;
        console = config.getConsole();

        if (console == null || !console.isEnabled()) return;

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
                ContextHelper.GetInstance.set(() -> Controller.this.moduleContext);
                exec0();
            } finally {
                ContextHelper.GetInstance.remove();
            }
        }

        private void exec0() {
            File consoleApp = FileUtil.newFile(moduleContext.getLibDir(), "module", "console");
            String docBase = consoleApp.getAbsolutePath();
            contextPath = console.getContextRoot();
            servletContainer.addWebapp(contextPath, docBase);
            logger.info("Open a browser to access the QingZhou console: http://localhost:" + console.getPort() + contextPath);
        }

        @Override
        public void undo() {
            servletContainer.removeApp(contextPath);
        }
    }
}
