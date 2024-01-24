package qingzhou.app.impl.filter;

import qingzhou.framework.api.ActionFilter;
import qingzhou.framework.api.AddModel;
import qingzhou.framework.api.AppContext;
import qingzhou.framework.api.ListModel;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;
import qingzhou.framework.console.I18NStore;

public class UniqueFilter implements ActionFilter {
    private final I18NStore i18NStore = new I18NStore();
    private volatile boolean i18nDone = false;

    @Override
    public String doFilter(Request request, Response response, AppContext appContext) throws Exception {
        initI18n();

        if (request.getActionName().equals(AddModel.ACTION_NAME_ADD)) {
            String id = request.getParameter(ListModel.FIELD_NAME_ID);
            boolean exists = appContext.getDataStore().exists(request.getModelName(), id);
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
