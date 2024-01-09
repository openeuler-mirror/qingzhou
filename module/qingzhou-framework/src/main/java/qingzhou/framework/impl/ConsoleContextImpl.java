package qingzhou.framework.impl;

import qingzhou.framework.api.ConsoleContext;
import qingzhou.framework.api.MenuInfo;
import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelAction;
import qingzhou.framework.api.ModelField;
import qingzhou.framework.api.ModelManager;
import qingzhou.framework.console.I18n;
import qingzhou.framework.console.Lang;
import qingzhou.framework.util.StringUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ConsoleContextImpl implements ConsoleContext {
    private final Map<String, String[]> langMap = new HashMap<>();
    private final Map<String, MenuInfoImpl> menuInfoMap = new HashMap<>();
    private final ModelManager modelManager;

    public ConsoleContextImpl(ModelManager modelManager) {
        this.modelManager = modelManager;
        initI18N();
    }

    private void initI18N() {
        for (String modelName : modelManager.getModelNames()) {
            final Model model = modelManager.getModel(modelName);

            // for i18n
            addI18N("model." + modelName, model.nameI18n());
            addI18N("model.info." + modelName, model.infoI18n());

            Arrays.stream(modelManager.getFieldNames(modelName)).forEach(k -> {
                ModelField v = modelManager.getModelField(modelName, k);
                addI18N("model.field." + modelName + "." + k, v.nameI18n());
                String[] info = v.infoI18n();
                if (info.length > 0) {
                    addI18N("model.field.info." + modelName + "." + k, info);
                }
            });

            modelManager.getMonitorFieldMap(modelName).forEach((k, v) -> {
                addI18N("model.field." + modelName + "." + k, v.nameI18n());
                String[] info = v.infoI18n();
                if (info.length > 0) {
                    addI18N("model.field.info." + modelName + "." + k, info);
                }
            });

            for (String actionName : modelManager.getActionNames(modelName)) {
                ModelAction modelAction = modelManager.getModelAction(modelName, actionName);
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
    public void setZhTrMap(char zh, char tr) {
        I18n.setZhTrMap(zh, tr);
    }

    @Override
    public void addI18N(String key, String[] i18n) {
        addI18N(key, i18n, true);
    }

    public void addI18N(String key, String[] i18n, boolean checkContainChinese) {
        Map<Lang, String> i18nMap = Lang.parseI18n(i18n);
        for (Lang lang : i18nMap.keySet()) {
            String val = i18nMap.get(lang);
            String[] indexedData = langMap.computeIfAbsent(key, s -> new String[Lang.values().length]);
            String old = indexedData[lang.ordinal()];
            if (old == null) {
                indexedData[lang.ordinal()] = val;
            } else { // 提醒更正会被覆盖的key
                new IllegalArgumentException("Duplicate i18n key: " + key + ", old: " + old + ", new: " + val).printStackTrace();
            }
        }

        // 防止将英文写成中文的情况发生
        if (checkContainChinese) {
            String val = langMap.get(key)[Lang.en.ordinal()];
            if (StringUtil.containsZHChar(val)) {
                new IllegalArgumentException("Please do not use Chinese in English: " + key).printStackTrace();
            }
        }
    }

    @Override
    public String getI18N(String key, Object... args) {
        return getI18N(I18n.getI18nLang(), key, args);
    }

    public String getI18N(Lang lang, String key, Object... args) {
        String[] values = langMap.get(key);
        if (values != null) {
            String s = values[lang.ordinal()];
            if (s != null && args != null && args.length > 0) {
                return String.format(s, args);
            }
            return s;
        }
        return null;

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
