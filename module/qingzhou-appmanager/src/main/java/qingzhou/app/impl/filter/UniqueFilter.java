package qingzhou.app.impl.filter;

import qingzhou.framework.I18NStore;
import qingzhou.framework.api.*;

public class UniqueFilter implements ActionFilter {
    private final I18NStore i18NStore = new I18NStore();
    private volatile boolean i18nDone = false;

    @Override
    public String doFilter(Request request, Response response, AppContext appContext) throws Exception {
        initI18n();

        if (request.getActionName().equals(AddModel.ACTION_NAME_ADD)) {
            String id = request.getParameter(ListModel.FIELD_NAME_ID);
            boolean exists = false;
            ModelManager modelManager = appContext.getConsoleContext().getModelManager();
            ModelBase modelInstance = modelManager.getModelInstance(request.getModelName());
            DataStore dataStore = modelInstance.getDataStore();
            if (dataStore != null) {
                exists = dataStore.exists(request.getModelName(), id);
            }
            if (exists) {
                return i18NStore.getI18N(request.getI18nLang(), "list.id.exists");
            }
        }

        return null;
    }

    private void initI18n() {
        if (i18nDone) return;
        synchronized (UniqueFilter.class) {
            if (i18nDone) return;
            i18nDone = true;

            i18NStore.addI18N("list.id.exists", new String[]{"数据已存在", "en:The data already exists"}, true);
        }
    }
}
