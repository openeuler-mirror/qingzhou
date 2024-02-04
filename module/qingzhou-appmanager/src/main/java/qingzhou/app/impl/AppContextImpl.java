package qingzhou.app.impl;

import qingzhou.framework.FrameworkContext;
import qingzhou.framework.InternalService;
import qingzhou.framework.api.ActionFilter;
import qingzhou.framework.api.AppContext;
import qingzhou.framework.api.ConsoleContext;
import qingzhou.framework.api.DataStore;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class AppContextImpl implements AppContext {
    private final FrameworkContext frameworkContext;
    private ConsoleContext consoleContext;
    private DataStore dataStore;
    private List<ActionFilter> actionFilters;

    public AppContextImpl(FrameworkContext frameworkContext) {
        this.frameworkContext = frameworkContext;
    }

    @Override
    public File getTemp(String subName) {
        return frameworkContext.getConfigManager().getTemp(subName);
    }

    @Override
    public Collection<Class<?>> getServiceTypes() {
        return frameworkContext.getServiceManager().getServiceTypes().stream().filter(aClass -> !isInternalService(aClass)).collect(Collectors.toSet());
    }

    @Override
    public <T> T getService(Class<T> serviceType) {
        if (isInternalService(serviceType)) return null;
        return frameworkContext.getServiceManager().getService(serviceType);
    }

    private boolean isInternalService(Class<?> serviceType) {
        return InternalService.class.isAssignableFrom(serviceType);
    }

    @Override
    public ConsoleContext getConsoleContext() {
        return consoleContext;
    }

    @Override
    public String getPlatformName() {
        return frameworkContext.getName();
    }

    @Override
    public String getPlatformVersion() {
        return frameworkContext.getVersion();
    }

    public void setConsoleContext(ConsoleContext consoleContext) {
        this.consoleContext = consoleContext;
    }

    @Override
    public void setDataStore(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public DataStore getDataStore() {
        return dataStore;
    }

    @Override
    public void addActionFilter(ActionFilter actionFilter) {
        if (actionFilters == null) {
            actionFilters = new ArrayList<>();
        }
        actionFilters.add(actionFilter);
    }

    public List<ActionFilter> getActionFilters() {
        return actionFilters;
    }
}
