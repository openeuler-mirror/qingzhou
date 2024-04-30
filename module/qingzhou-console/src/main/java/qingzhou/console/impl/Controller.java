package qingzhou.console.impl;

import qingzhou.config.ConfigService;
import qingzhou.config.Console;
import qingzhou.config.Module;
import qingzhou.console.ContextHelper;
import qingzhou.crypto.CryptoService;
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

@qingzhou.engine.Module
public class Controller implements ModuleActivator {
    @Service
    private Logger logger;
    @Service
    private ConfigService configService;
    @Service
    private ServletService servletService;
    @Service
    private CryptoService cryptoService; // war 里需要

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
        Module module = configService.getModule();
        console = module.getConsole();

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
            try {
                ContextHelper.GetInstance.set(new ContextHelper() {
                    @Override
                    public <T> T getService(Class<T> type) {
                        List<Field> collect = Arrays.stream(Controller.class.getDeclaredFields()).filter(field -> field.getType() == type).collect(Collectors.toList());
                        try {
                            Field field = collect.get(0);
                            field.setAccessible(true);
                            return (T) field.get(Controller.this.instance);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
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
