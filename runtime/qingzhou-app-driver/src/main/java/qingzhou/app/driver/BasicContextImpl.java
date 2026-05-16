package qingzhou.app.driver;

import java.io.File;
import java.util.Properties;

import qingzhou.api.BasicContext;

class BasicContextImpl implements BasicContext {
    private final AppContextImpl appContext;

    BasicContextImpl(AppContextImpl appContext) {
        this.appContext = appContext;
    }

    @Override
    public String getVersion() {
        return appContext.getVersion();
    }

    @Override
    public Properties getProperties() {
        return appContext.getProperties();
    }

    @Override
    public File getBase() {
        return appContext.getBase();
    }

    @Override
    public File getTemp() {
        return appContext.getTemp();
    }
}
