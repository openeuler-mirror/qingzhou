package qingzhou.framework.impl.app;

import qingzhou.api.console.ConsoleContext;
import qingzhou.api.console.DataStore;
import qingzhou.api.console.Model;
import qingzhou.api.console.ModelAction;
import qingzhou.api.console.ModelManager;
import qingzhou.framework.impl.ServerUtil;
import qingzhou.framework.app.I18n;
import qingzhou.framework.app.Lang;
import qingzhou.framework.impl.app.model.ModelManagerImpl;

import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ConsoleContextImpl implements ConsoleContext {
    private final Map<Lang, Map<String, String>> langMap = new HashMap<>();
    private final Map<String, MenuInfo> menuInfoMap = new HashMap<>();
    private ModelManagerImpl modelManager;
    private DataStore dataStore;

    public ConsoleContextImpl(URLClassLoader classLoader) throws Exception {
        if (classLoader == null) return; // null is master

        modelManager = new ModelManagerImpl(classLoader);
        modelManager.init();
        initI18N();
    }

    private void initI18N() {
        for (String modelName : modelManager.getAllModelNames()) {
            final Model model = modelManager.getModel(modelName);

            // for i18n
            addI18N("model." + modelName, model.nameI18n());
            addI18N("model.info." + modelName, model.infoI18n());

            modelManager.getModelFieldMap(modelName).forEach((k, v) -> {
                addI18N("model.field." + modelName + "." + k, v.nameI18n());
                String[] info = v.infoI18n();
                if (info.length > 0) {
                    addI18N("model.field.info." + modelName + "." + k, info);
                }
            });

            modelManager.getModelMonitoringFieldMap(modelName).forEach((k, v) -> {
                addI18N("model.field." + modelName + "." + k, v.nameI18n());
                String[] info = v.infoI18n();
                if (info.length > 0) {
                    addI18N("model.field.info." + modelName + "." + k, info);
                }
            });

            for (ModelAction modelAction : modelManager.getModelActions(modelName)) {
                addI18N("model.action." + modelName + "." + modelAction.name(), modelAction.nameI18n());
                addI18N("model.action.info." + modelName + "." + modelAction.name(), modelAction.infoI18n());
            }
        }
    }

    @Override
    public ModelManager getModelManager() {
        return modelManager;
    }

    @Override
    public void addI18N(String key, String[] i18n) {
        addI18N(key, i18n, true);
    }

    @Override
    public void addI18N(String key, String[] i18n, boolean checkContainChinese) {
        Map<Lang, String> i18nMap = Lang.parseI18n(i18n);
        for (Lang lang : i18nMap.keySet()) {
            String val = i18nMap.get(lang);
            String old = langMap.computeIfAbsent(lang, lang1 -> new HashMap<>()).put(key, val);
            if (old != null) { // 提醒更正会被覆盖的key
                new IllegalArgumentException("Duplicate i18n key: " + key + ", old: " + old + ", new: " + val).printStackTrace();
            }
        }

        // 防止将英文写成中文的情况发生
        if (checkContainChinese) {
            String val = langMap.get(Lang.en).get(key);
            if (ServerUtil.containsZHChar(val)) {
                new IllegalArgumentException("Please do not use Chinese in English: " + key).printStackTrace();
            }
        }
    }

    @Override
    public String getI18N(String key, Object... args) {
        return getI18N(I18n.getI18nLang(), key, args);
    }

    @Override
    public void setDataStore(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public DataStore getDataStore() {
        return dataStore;
    }

    public String getI18N(Lang lang, String key, Object... args) {
        String s = langMap.get(lang).get(key);
        if (s != null && args != null && args.length > 0) {
            return String.format(s, args);
        }

        return s;
    }

    @Override
    public void setMenuInfo(String menuName, String[] menuI18n, String menuIcon, int menuOrder) {
        MenuInfo menuInfo = menuInfoMap.computeIfAbsent(menuName, s -> new MenuInfo(menuName));
        menuInfo.menuI18n = menuI18n;
        menuInfo.menuIcon = menuIcon;
        menuInfo.menuOrder = menuOrder;
    }

    public MenuInfo getMenuInfo(String menuName) {
        return menuInfoMap.get(menuName);
    }

    public static class MenuInfo {
        private final String menuName;
        private String[] menuI18n;
        private String menuIcon;
        private int menuOrder;

        public String[] getMenuI18n() {
            return menuI18n;
        }

        public String getMenuIcon() {
            return menuIcon;
        }

        public int getMenuOrder() {
            return menuOrder;
        }

        private MenuInfo(String menuName) {
            this.menuName = menuName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MenuInfo menuInfo = (MenuInfo) o;
            return Objects.equals(menuName, menuInfo.menuName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(menuName);
        }
    }
}
