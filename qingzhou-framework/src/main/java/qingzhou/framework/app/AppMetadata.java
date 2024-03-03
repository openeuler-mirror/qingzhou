package qingzhou.framework.app;

import qingzhou.api.Lang;

import java.util.Properties;

public interface AppMetadata {
    String getName();

    Properties getProperties();

    String getI18n(Lang lang, String key, Object... args);

    Menu getMenu(String menuName);

    ModelManager getModelManager();
}
