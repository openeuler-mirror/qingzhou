package qingzhou.app;

import qingzhou.api.Lang;
import qingzhou.api.metadata.AppMetadata;
import qingzhou.api.metadata.MenuData;
import qingzhou.api.metadata.ModelManager;
import qingzhou.framework.app.I18nTool;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class AppMetadataImpl implements AppMetadata, Serializable {
    private final I18nTool i18nTool = new I18nTool();
    private final Map<String, String> appProperties = new HashMap<>();
    private final ModelManagerImpl modelManager = new ModelManagerImpl();
    private final Map<String, MenuDataImpl> menus = new HashMap<>();
    private String appName;

    @Override
    public String getName() {
        return appName;
    }

    @Override
    public Map<String, String> getConfig() {
        return appProperties;
    }

    @Override
    public String getI18n(Lang lang, String key, Object... args) {
        return i18nTool.getI18n(lang, key, args);
    }

    @Override
    public MenuData getMenu(String menuName) {
        return menus.get(menuName);
    }

    @Override
    public ModelManager getModelManager() {
        return modelManager;
    }

    public void addMenu(String menuName, String[] menuI18n, String menuIcon, int menuOrder) {
        MenuDataImpl menu = menus.computeIfAbsent(menuName, s -> new MenuDataImpl(menuName));
        menu.setMenuI18n(menuI18n);
        menu.setMenuIcon(menuIcon);
        menu.setMenuOrder(menuOrder);
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void addI18n(String key, String[] i18n) {
        i18nTool.addI18n(key, i18n, true);
    }
}
