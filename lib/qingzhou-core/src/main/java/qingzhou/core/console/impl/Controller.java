package qingzhou.core.console.impl;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import qingzhou.config.console.Console;
import qingzhou.core.AppPageData;
import qingzhou.core.console.ContextHelper;
import qingzhou.core.console.servlet.ServletContainer;
import qingzhou.core.console.servlet.impl.ServletContainerImpl;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Process;
import qingzhou.engine.util.pattern.ProcessSequence;
import qingzhou.json.Json;
import qingzhou.logger.Logger;

public class Controller implements Process {
    public final ModuleContext moduleContext;
    private Console console;
    private String hostIp;
    private ProcessSequence sequence;
    private ServletContainer servletContainer;

    public Controller(ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }

    @Override
    public void exec() throws Throwable {
        Json json = moduleContext.getService(Json.class);
        String consoleJson = json.toJson(((Map<String, Object>) moduleContext.getConfig()).get("console"));
        console = json.fromJson(consoleJson, Console.class);
        if (console == null || !console.isEnabled()) return;

        Map<String, String> agentConfig = (Map<String, String>) ((Map<String, Object>) moduleContext.getConfig()).get("agent");
        if (agentConfig != null) {
            hostIp = agentConfig.get("agentHost");
        }
        if (Utils.isBlank(hostIp)) hostIp = "localhost";

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
        public void exec() throws Throwable {
            servletContainer = new ServletContainerImpl();
            servletContainer.start(console.getPort(),
                    new File(moduleContext.getTemp(), "servlet"),
                    new Properties() {{
                        setProperty("maxPostSize", String.valueOf(console.getMaxPostSize()));
                        setProperty("enabledRemoteIpValve", String.valueOf(console.isEnabledRemoteIpValve()));
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
            contextPath = console.getContextRoot();
            servletContainer.addWebapp(contextPath, docBase, new Properties() {{
                setProperty("webResources", "/=" + FileUtil.newFile(moduleContext.getTemp(), AppPageData.DOWNLOAD_PAGE_ROOT_DIR).getAbsoluteFile());
            }});
            moduleContext.getService(Logger.class).info("Open a browser to access the Qingzhou console: http://" + hostIp + ":" + console.getPort() + contextPath);
        }

        @Override
        public void undo() {
            servletContainer.removeApp(contextPath);
        }
    }
}
