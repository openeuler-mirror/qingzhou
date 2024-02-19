package qingzhou.console;

import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.framework.app.App;
import qingzhou.framework.api.ConsoleContext;
import qingzhou.framework.api.Lang;
import qingzhou.framework.api.MenuInfo;
import qingzhou.framework.api.ModelManager;

import java.util.HashMap;
import java.util.Map;

public class AppStubManager {
    private static final AppStubManager instance = new AppStubManager();

    public static AppStubManager getInstance() {
        return instance;
    }

    private final Map<String, AppStub> appStubMap = new HashMap<>();

    public void registerAppStub(String appToken, AppStub appStub) {
        appStubMap.put(appToken, appStub);
    }

    // todo: unregisterAppStub 何时调用？
    public void unregisterAppStub(String appToken) {
        appStubMap.remove(appToken);
    }

    public AppStub getAppStub(String appToken) {
        return appStubMap.computeIfAbsent(appToken, s -> {
            App localApp = ConsoleWarHelper.getLocalApp(appToken);
            if (localApp == null) return null;

            return new AppStub() {
                private final ConsoleContext localAppConsole = localApp.getAppContext().getConsoleContext();

                @Override
                public ModelManager getModelManager() {
                    return localAppConsole.getModelManager();
                }

                @Override
                public String getI18N(Lang lang, String key, Object... args) {
                    return localAppConsole.getI18N(lang, key, args);
                }

                @Override
                public MenuInfo getMenuInfo(String menuName) {
                    return localAppConsole.getMenuInfo(menuName);
                }
            };
        });
    }
}
