package qingzhou.api;

import java.util.Properties;

public interface AppMetadata {
    String getName();

    Properties getProperties();

    String getI18N(Lang lang, String key, Object... args);

    Menu getMenu(String menuName);

    ModelManager getModelManager();
}
