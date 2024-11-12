package qingzhou.console.impl;

import qingzhou.config.Config;
import qingzhou.config.Console;
import qingzhou.console.ContextHelper;
import qingzhou.crypto.CryptoService;
import qingzhou.deployer.ActionInvoker;
import qingzhou.deployer.AppPageData;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.JmxServiceAdapter;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.Resource;
import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.pattern.Process;
import qingzhou.engine.util.pattern.ProcessSequence;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.Registry;
import qingzhou.servlet.ServletContainer;
import qingzhou.servlet.ServletService;

import java.io.File;
import java.util.Properties;

@Module
public class Controller implements ModuleActivator {
    @Resource
    private Logger logger;
    @Resource
    private Config config;
    @Resource
    private ServletService servletService;
    @Resource
    private Deployer deployer;
    @Resource
    private Registry registry;
    @Resource
    private CryptoService cryptoService;
    @Resource
    private Json json;// RemoteClient会用到
    @Resource(optional = true) // console 禁用（远程受管实例）后，此对象为 null，optional = true
    private JmxServiceAdapter jmxServiceAdapter;
    @Resource
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
            logger.info("Open a browser to access the Qingzhou console: http://localhost:" + console.getWeb().getPort() + contextPath);
        }

        @Override
        public void undo() {
            servletContainer.removeApp(contextPath);
        }
    }
}
