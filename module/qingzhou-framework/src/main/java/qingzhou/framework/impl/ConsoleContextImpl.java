package qingzhou.framework.impl;

import qingzhou.framework.api.*;
import qingzhou.framework.console.I18n;
import qingzhou.framework.console.Lang;
import qingzhou.framework.util.ServerUtil;

import java.util.HashMap;
import java.util.Map;

public class ConsoleContextImpl implements ConsoleContext {
    private final Map<Lang, Map<String, String>> langMap = new HashMap<>();
    private final Map<String, MenuInfoImpl> menuInfoMap = new HashMap<>();
    private DataStore dataStore;

    public ConsoleContextImpl(ModelManager modelManager) {
        initI18N(modelManager);
    }

    private void initI18N(ModelManager modelManager) {
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
    public void addI18N(String key, String[] i18n) {
        addI18N(key, i18n, true);
    }

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
        MenuInfoImpl menuInfo = menuInfoMap.computeIfAbsent(menuName, s -> new MenuInfoImpl(menuName));
        menuInfo.setMenuI18n(menuI18n);
        menuInfo.setMenuIcon(menuIcon);
        menuInfo.setMenuOrder(menuOrder);
    }

    @Override
    public MenuInfo getMenuInfo(String menuName) {
        return menuInfoMap.get(menuName);
    }
}
