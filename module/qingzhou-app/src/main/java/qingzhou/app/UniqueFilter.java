package qingzhou.app;

import qingzhou.api.*;
import qingzhou.api.type.Createable;
import qingzhou.framework.console.I18nTool;

public class UniqueFilter implements ActionFilter {
    private final I18nTool i18nTool = new I18nTool();
    private volatile boolean i18nDone = false;

    @Override
    public String doFilter(Request request, Response response, AppContext appContext) throws Exception {
        initI18n();

        if (request.getActionName().equals(Createable.ACTION_NAME_ADD)) {
            ModelBase modelInstance = Controller.appManager.getApp(request.getAppName()).getModelInstance(request.getModelName());
            DataStore dataStore = modelInstance.getDataStore();
            if (dataStore.exists(request.getModelName(), request.getId())) {
                String modelNameI18n = appContext.getAppMetadata().getI18n(request.getI18nLang(), "model." + request.getModelName());
                return i18nTool.getI18n(request.getI18nLang(), "list.id.exists", modelNameI18n);
            }
        }

        return null;
    }

    private void initI18n() {
        if (i18nDone) return;

        synchronized (UniqueFilter.class) {
            if (i18nDone) return;
            i18nDone = true;

            i18nTool.addI18n("list.id.exists", new String[]{"%s已存在", "en:%s already exists"}, true);
        }
    }
}
