package qingzhou.console;

import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.ConsoleContext;
import qingzhou.framework.api.Lang;
import qingzhou.framework.api.MenuInfo;
import qingzhou.framework.api.ModelManager;

import java.util.HashMap;
import java.util.Map;

public class AppStubManager {
    private final FrameworkContext frameworkContext;
    private final Map<String, AppStub> appStubMap = new HashMap<>();

    public AppStubManager(FrameworkContext frameworkContext) {
        this.frameworkContext = frameworkContext;
    }

    public void registerAppStub(String appToken, AppStub appStub) {
        appStubMap.put(appToken, appStub);
    }

    public void unregisterAppStub(String appToken) {
        appStubMap.remove(appToken);
    }

    public AppStub getAppStub(String appToken) {
        return appStubMap.computeIfAbsent(appToken, s -> {
            ConsoleContext consoleContext = frameworkContext.getAppManager().getApp(appToken).getAppContext().getConsoleContext();
            return new AppStub() {
                @Override
                public ModelManager getModelManager() {
                    return consoleContext.getModelManager();
                }

                @Override
                public String getI18N(Lang lang, String key, Object... args) {
                    return consoleContext.getI18N(lang, key, args);
                }

                @Override
                public MenuInfo getMenuInfo(String menuName) {
                    return consoleContext.getMenuInfo(menuName);
                }
            };
        });
    }
}
