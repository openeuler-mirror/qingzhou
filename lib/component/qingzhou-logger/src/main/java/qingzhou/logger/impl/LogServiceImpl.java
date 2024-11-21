package qingzhou.logger.impl;

import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.FileUtil;
import qingzhou.logger.LogService;
import qingzhou.logger.Logger;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

public class LogServiceImpl implements LogService {
    public static final String APP_NAME = "qingzhou.appName";

    private final Map<ClassLoader, Logger> loggers = new HashMap<>();
    private final ModuleContext context;

    public LogServiceImpl(ModuleContext context) {
        this.context = context;
    }

    @Override
    public Logger getLogger(Class<?> clazz) {
        ClassLoader classLoader = clazz.getClassLoader();
        return loggers.computeIfAbsent(classLoader, cl -> {
            String property = System.getProperty(APP_NAME);
            try {
                setAppName(cl);
                URL tinylogUrl = FileUtil.newFile(context.getLibDir(), "plugins", "qingzhou-logger-tinylog.jar").toURI().toURL();
                URLClassLoader loader = new URLClassLoader(new URL[]{tinylogUrl}, cl);
                return (Logger) loader.loadClass("qingzhou.logger.impl.LoggerImpl").newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (property == null) {
                    System.clearProperty(APP_NAME);
                } else {
                    System.setProperty(APP_NAME, property);
                }
            }
        });
    }

    @Override
    public void remove(ClassLoader classLoader) {
        loggers.remove(classLoader);
    }

    private void setAppName(ClassLoader classLoader) {
        URL resource = classLoader.getResource("");
        String appName = "";
        if (resource != null) {
            appName = new File(resource.getFile()).getName();
        }
        System.setProperty(APP_NAME, appName);
    }
}
