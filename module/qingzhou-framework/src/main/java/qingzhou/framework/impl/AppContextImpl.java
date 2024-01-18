package qingzhou.framework.impl;

import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.ActionFilter;
import qingzhou.framework.api.AppContext;
import qingzhou.framework.api.ConsoleContext;
import qingzhou.framework.api.DataStore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AppContextImpl implements AppContext {
    private final FrameworkContext frameworkContext;
    private String appName;
    private ConsoleContext consoleContext;
    private DataStore dataStore;
    private List<ActionFilter> actionFilters;

    public AppContextImpl(FrameworkContextImpl frameworkContext) {
        this.frameworkContext = frameworkContext;
    }

    @Override
    public File getAppDomain() {
        return new File(frameworkContext.getDomain(), getAppName());
    }

    @Override
    public File getAppCache() {
        return frameworkContext.getCache(getAppDomain());
    }

    @Override
    public File getAppLogs() {
        return new File(getAppDomain(), "logs");
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
    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    @Override
    public ConsoleContext getConsoleContext() {
        return consoleContext;
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
