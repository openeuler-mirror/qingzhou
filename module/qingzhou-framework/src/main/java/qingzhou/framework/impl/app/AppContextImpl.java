package qingzhou.framework.impl.app;

import qingzhou.api.AppContext;
import qingzhou.api.console.ConsoleContext;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.impl.FrameworkContextImpl;
import qingzhou.framework.impl.ServerUtil;

import java.io.File;
import java.util.Set;

public class AppContextImpl implements AppContext {
    private final FrameworkContext frameworkContext;
    private ConsoleContextImpl consoleContext;

    public AppContextImpl(FrameworkContextImpl frameworkContext) {
        this.frameworkContext = frameworkContext;
    }

    @Override
    public File getDomain() {
        return ServerUtil.getDomain();
    }

    @Override
    public File getTemp() {
        return ServerUtil.getTempDir();
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
    public ConsoleContext getConsoleContext() {
        return consoleContext;
    }

    public void setConsoleContext(ConsoleContextImpl consoleContext) {
        this.consoleContext = consoleContext;
    }
}
