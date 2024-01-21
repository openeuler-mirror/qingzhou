package qingzhou.app.impl;

import qingzhou.framework.api.AppStub;
import qingzhou.framework.api.Lang;
import qingzhou.framework.api.MenuInfo;
import qingzhou.framework.api.ModelManager;
import qingzhou.framework.console.I18NStore;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class AppStubImpl implements AppStub, Serializable {
    protected ModelManager modelManager;
    protected I18NStore i18NStore = new I18NStore();
    protected Map<String, MenuInfoImpl> menuInfoMap = new HashMap<>();

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
}
