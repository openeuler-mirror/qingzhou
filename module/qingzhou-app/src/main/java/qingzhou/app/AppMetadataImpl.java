package qingzhou.app;

import qingzhou.api.AppMetadata;
import qingzhou.api.Lang;
import qingzhou.api.Menu;
import qingzhou.api.ModelManager;
import qingzhou.framework.app.I18NStore;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class AppMetadataImpl implements AppMetadata, Serializable {
    private final I18NStore i18NStore = new I18NStore();
    private final Properties appProperties = new Properties();
    private final ModelManagerImpl modelManager = new ModelManagerImpl();
    private final Map<String, MenuImpl> menus = new HashMap<>();
    private String appName;

    @Override
    public String getName() {
        return appName;
    }

    @Override
    public Properties getProperties() {
        return appProperties;
    }

    @Override
    public String getI18N(Lang lang, String key, Object... args) {
        return i18NStore.getI18N(lang, key, args);
    }

    @Override
    public Menu getMenu(String menuName) {
        return menus.get(menuName);
    }

    @Override
    public ModelManager getModelManager() {
        return modelManager;
    }

    public void addMenu(String menuName, String[] menuI18n, String menuIcon, int menuOrder) {
        MenuImpl menu = menus.computeIfAbsent(menuName, s -> new MenuImpl(menuName));
        menu.setMenuI18n(menuI18n);
        menu.setMenuIcon(menuIcon);
        menu.setMenuOrder(menuOrder);
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void addI18N(String key, String[] i18n) {
        i18NStore.addI18N(key, i18n, true);
    }
}
