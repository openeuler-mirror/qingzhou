package qingzhou.console.impl;

import qingzhou.config.Config;
import qingzhou.config.ConfigService;
import qingzhou.config.Console;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.Service;
import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.pattern.Process;
import qingzhou.engine.util.pattern.ProcessSequence;
import qingzhou.logger.Logger;
import qingzhou.servlet.ServletContainer;
import qingzhou.servlet.ServletService;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Module
public class Controller implements ModuleActivator {
    public static ModuleContext moduleContext;
    private static Controller instance;

    public static <T> T getService(Class<T> type) {
        List<Field> collect = Arrays.stream(Controller.class.getDeclaredFields()).filter(field -> field.getType() == type).collect(Collectors.toList());
        try {
            return (T) collect.get(0).get(instance);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public Controller() {
        instance = this;
    }

    @Service
    private Logger logger;
    @Service
    private ConfigService configService;
    @Service
    private ServletService servletService;

    private Console console;
    private ProcessSequence sequence;
    private ServletContainer servletContainer;

    @Override
    public void start(ModuleContext context) throws Exception {
        moduleContext = context;
        Config config = configService.getConfig();
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
                    moduleContext.getTemp().getAbsolutePath());
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
            File consoleApp = FileUtil.newFile(moduleContext.getLibDir(), "module", "console");
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
