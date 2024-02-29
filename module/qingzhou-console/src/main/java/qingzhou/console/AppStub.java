package qingzhou.console;

import qingzhou.api.Lang;
import qingzhou.api.MenuInfo;
import qingzhou.api.ModelManager;

public interface AppStub {
    ModelManager getModelManager();

    String getI18N(Lang lang, String key, Object... args);

    MenuInfo getMenuInfo(String menuName);
}
