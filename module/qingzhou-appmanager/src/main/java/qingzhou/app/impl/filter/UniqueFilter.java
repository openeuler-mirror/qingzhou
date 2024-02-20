package qingzhou.app.impl.filter;

import qingzhou.framework.app.I18NStore;
import qingzhou.framework.app.RequestImpl;
import qingzhou.framework.app.ResponseImpl;
import qingzhou.framework.api.ActionFilter;
import qingzhou.framework.api.AddModel;
import qingzhou.framework.api.AppContext;
import qingzhou.framework.api.ListModel;
import qingzhou.framework.api.ModelBase;
import qingzhou.framework.api.ModelManager;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;
import qingzhou.framework.util.StringUtil;

public class UniqueFilter implements ActionFilter {
    private final I18NStore i18NStore = new I18NStore();
    private volatile boolean i18nDone = false;

    @Override
    public String doFilter(Request request, Response response, AppContext appContext) throws Exception {
        initI18n();

        if (request.getActionName().equals(AddModel.ACTION_NAME_ADD)) {
            ModelManager modelManager = appContext.getConsoleContext().getModelManager();
            ModelBase modelInstance = modelManager.getModelInstance(request.getModelName());

            RequestImpl requestTemp = ((RequestImpl) request).clone();
            if (StringUtil.isBlank(requestTemp.getParameter(ListModel.FIELD_NAME_ID))) {
                requestTemp.setId(modelInstance.resolveId(requestTemp));
            }
            Response responseTemp = new ResponseImpl();
            modelInstance.show(requestTemp, responseTemp);
            boolean exists = responseTemp.isSuccess() && !responseTemp.getDataList().isEmpty();
            if (exists) {
                String modelNameI18n = appContext.getConsoleContext().getI18N(request.getI18nLang(), "model." + request.getModelName());
                return i18NStore.getI18N(request.getI18nLang(), "list.id.exists", modelNameI18n);
            }
        }

        return null;
    }

    private void initI18n() {
        if (i18nDone) return;
        synchronized (UniqueFilter.class) {
            if (i18nDone) return;
            i18nDone = true;

            i18NStore.addI18N("list.id.exists", new String[]{"%s已存在", "en:%s already exists"}, true);
        }
    }
}
