package qingzhou.console;

import qingzhou.api.Lang;
import qingzhou.api.Menu;
import qingzhou.api.ModelManager;

public interface AppStub {
    ModelManager getModelManager();

    String getI18n(Lang lang, String key, Object... args);

    Menu getMenu(String menuName);
}
