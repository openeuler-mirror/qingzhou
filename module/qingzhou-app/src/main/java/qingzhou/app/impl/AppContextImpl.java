package qingzhou.app.impl;

import qingzhou.api.ActionFilter;
import qingzhou.api.AppContext;
import qingzhou.api.ConsoleContext;
import qingzhou.api.DataStore;
import qingzhou.framework.Framework;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AppContextImpl implements AppContext {
    private final Framework framework;
    private ConsoleContext consoleContext;
    private DataStore dataStore;
    private List<ActionFilter> actionFilters;
    private String appName;

    public AppContextImpl(Framework framework) {
        this.framework = framework;
    }

    @Override
    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    @Override
    public File getTemp() {
        return framework.getTemp(this.appName);
    }

    @Override
    public ConsoleContext getConsoleContext() {
        return consoleContext;
    }

    @Override
    public String getPlatformName() {
        return framework.getName();
    }

    @Override
    public String getPlatformVersion() {
        return framework.getVersion();
    }

    public void setConsoleContext(ConsoleContext consoleContext) {
        this.consoleContext = consoleContext;
    }

    @Override
    public void setDefaultDataStore(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public DataStore getDefaultDataStore() {
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
