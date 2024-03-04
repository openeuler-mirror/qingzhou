package qingzhou.console;

import qingzhou.api.Lang;
import qingzhou.api.metadata.MenuData;
import qingzhou.api.metadata.ModelManager;

public interface AppStub {
    ModelManager getModelManager();

    String getI18n(Lang lang, String key, Object... args);

    MenuData getMenu(String menuName);
}
