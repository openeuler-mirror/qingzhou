package qingzhou.api.metadata;

import qingzhou.api.Lang;

import java.util.Map;

public interface AppMetadata {
    String getName();

    Map<String, String> getConfig();

    String getI18n(Lang lang, String key, Object... args);

    MenuData getMenu(String menuName);

    ModelManager getModelManager();
}
