package qingzhou.framework.impl;

import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.AppContext;
import qingzhou.framework.api.ConsoleContext;
import qingzhou.framework.api.ModelManager;

import java.io.File;
import java.util.Set;

public class AppContextImpl implements AppContext {
    private final FrameworkContext frameworkContext;
    private ConsoleContext consoleContext;
    private ModelManager modelManager;

    public AppContextImpl(FrameworkContextImpl frameworkContext) {
        this.frameworkContext = frameworkContext;
    }

    @Override
    public File getDomain() {
        return frameworkContext.getDomain();
    }

    @Override
    public File getCache() {
        return frameworkContext.getCache();
    }

    @Override
    public File getHome() {
        return frameworkContext.getHome();
    }

    @Override
    public Set<Class<?>> getServiceTypes() {
        return frameworkContext.getServiceTypes();
    }

    @Override
    public <T> T getService(Class<T> serviceType) {
        return frameworkContext.getService(serviceType);
    }

    @Override
    public ModelManager getModelManager() {
        return modelManager;
    }

    public void setModelManager(ModelManager modelManager) {
        this.modelManager = modelManager;
    }

    @Override
    public ConsoleContext getConsoleContext() {
        return consoleContext;
    }

    public void setConsoleContext(ConsoleContext consoleContext) {
        this.consoleContext = consoleContext;
    }
}
