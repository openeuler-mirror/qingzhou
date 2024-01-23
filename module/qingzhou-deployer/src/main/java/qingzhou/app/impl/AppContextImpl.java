package qingzhou.app.impl;

import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.ActionFilter;
import qingzhou.framework.api.AppContext;
import qingzhou.framework.api.ConsoleContext;
import qingzhou.framework.api.DataStore;
import qingzhou.framework.api.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AppContextImpl implements AppContext {
    private final FrameworkContext frameworkContext;
    private ConsoleContext consoleContext;
    private DataStore dataStore;
    private List<ActionFilter> actionFilters;

    public AppContextImpl(FrameworkContext frameworkContext) {
        this.frameworkContext = frameworkContext;
    }

    @Override
    public Logger getLogger() {
        return frameworkContext.getLogger();
    }

    @Override
    public File getTemp(String subName) {
        return frameworkContext.getFileManager().getTemp(subName);
    }

    @Override
    public File getDomain() {
        return frameworkContext.getFileManager().getDomain();
    }

    @Override
    public File getHome() {
        return frameworkContext.getFileManager().getHome();
    }

    @Override
    public <T> T getService(Class<T> serviceType) {
        return frameworkContext.getServiceManager().getService(serviceType);
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
