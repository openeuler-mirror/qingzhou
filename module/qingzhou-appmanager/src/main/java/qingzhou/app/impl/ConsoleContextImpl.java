package qingzhou.app.impl;

import qingzhou.framework.I18NStore;
import qingzhou.framework.api.ConsoleContext;
import qingzhou.framework.api.Lang;
import qingzhou.framework.api.MenuInfo;
import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelAction;
import qingzhou.framework.api.ModelField;
import qingzhou.framework.api.ModelManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ConsoleContextImpl implements ConsoleContext {
    private ModelManager modelManager;
    private final I18NStore i18NStore = new I18NStore();
    private final Map<String, MenuInfo> menuInfoMap = new HashMap<>();
    private String entryModel;

    @Override
    public ModelManager getModelManager() {
        return modelManager;
    }

    @Override
    public String getI18N(Lang lang, String key, Object... args) {
        return i18NStore.getI18N(lang, key, args);
    }

    @Override
    public MenuInfo getMenuInfo(String menuName) {
        return menuInfoMap.get(menuName);
    }

    public void setModelManager(ModelManager modelManager) {
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

            for (String actionName : modelManager.getActionNames(modelName)) {
                ModelAction modelAction = modelManager.getModelAction(modelName, actionName);
                addI18N("model.action." + modelName + "." + modelAction.name(), modelAction.nameI18n());
                addI18N("model.action.info." + modelName + "." + modelAction.name(), modelAction.infoI18n());
            }
        }
    }

    @Override
    public void addI18N(String key, String[] i18n) {
        i18NStore.addI18N(key, i18n, true);
    }

    @Override
    public void setMenuInfo(String menuName, String[] menuI18n, String menuIcon, int menuOrder) {
        MenuInfoImpl menuInfo = (MenuInfoImpl) menuInfoMap.computeIfAbsent(menuName, s -> new MenuInfoImpl(menuName));
        menuInfo.setMenuI18n(menuI18n);
        menuInfo.setMenuIcon(menuIcon);
        menuInfo.setMenuOrder(menuOrder);
    }
}
