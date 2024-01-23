package qingzhou.framework;

import qingzhou.framework.api.Lang;
import qingzhou.framework.api.MenuInfo;
import qingzhou.framework.api.ModelManager;

public interface AppStub {
    ModelManager getModelManager();

    String getI18N(Lang lang, String key, Object... args);

    MenuInfo getMenuInfo(String menuName);

    String getEntryModel();
}
