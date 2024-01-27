package qingzhou.console;

import qingzhou.framework.I18NStore;
import qingzhou.framework.api.Lang;
import qingzhou.framework.api.MenuInfo;
import qingzhou.framework.api.ModelManager;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class AppStub implements Serializable {
    private ModelManager modelManager;
    private final I18NStore i18NStore = new I18NStore();
    private final Map<String, MenuInfo> menuInfoMap = new HashMap<>();
    private String entryModel;

    public ModelManager getModelManager() {
        return modelManager;
    }

    public String getI18N(Lang lang, String key, Object... args) {
        return i18NStore.getI18N(lang, key, args);
    }

    public MenuInfo getMenuInfo(String menuName) {
        return menuInfoMap.get(menuName);
    }

    public String getEntryModel() {
        return entryModel;
    }

    public void setModelManager(ModelManager modelManager) {
        this.modelManager = modelManager;
    }

    public void setEntryModel(String entryModel) {
        this.entryModel = entryModel;
    }
}
