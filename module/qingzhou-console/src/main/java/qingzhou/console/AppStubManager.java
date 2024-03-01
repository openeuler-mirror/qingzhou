package qingzhou.console;

import qingzhou.api.Lang;
import qingzhou.api.Menu;
import qingzhou.api.ModelManager;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.framework.app.App;

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
                @Override
                public ModelManager getModelManager() {
                    return localApp.getAppContext().getConsoleContext().getModelManager();
                }

                @Override
                public String getI18N(Lang lang, String key, Object... args) {
                    return localApp.getAppContext().getMetadata().getI18N(lang, key, args);
                }

                @Override
                public Menu getMenu(String menuName) {
                    return localApp.getAppContext().getMetadata().getMenu(menuName);
                }
            };
        });
    }
}
