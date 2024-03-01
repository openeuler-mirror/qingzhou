package qingzhou.app;

import qingzhou.api.*;
import qingzhou.bootstrap.main.FrameworkContext;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AppContextImpl implements AppContext {
    private final FrameworkContext frameworkContext;
    private final AppMetadataImpl metadata;
    private ConsoleContext consoleContext;
    private DataStore dataStore;
    private List<ActionFilter> actionFilters;

    public AppContextImpl(FrameworkContext frameworkContext) {
        this.frameworkContext = frameworkContext;
        this.metadata = new AppMetadataImpl();
    }

    @Override
    public File getTemp() {
        return frameworkContext.getTemp(metadata.getName());
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
    public void setDefaultDataStore(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public DataStore getDefaultDataStore() {
        return dataStore;
    }

    @Override
    public void addI18N(String key, String[] i18n) {
        metadata.addI18N(key, i18n);
    }

    @Override
    public void addMenu(String menuName, String[] menuI18n, String menuIcon, int menuOrder) {
        metadata.addMenu(menuName, menuI18n, menuIcon, menuOrder);
    }

    @Override
    public AppMetadata getMetadata() {
        return metadata;
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
