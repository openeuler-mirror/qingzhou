package qingzhou.app;

import qingzhou.api.*;
import qingzhou.framework.app.I18nTool;
import qingzhou.framework.app.RequestImpl;
import qingzhou.framework.util.StringUtil;

public class UniqueFilter implements ActionFilter {
    private final I18nTool i18nTool = new I18nTool();
    private volatile boolean i18nDone = false;

    @Override
    public String doFilter(Request request, Response response, AppContext appContext) throws Exception {
        initI18n();

        if (request.getActionName().equals(AddModel.ACTION_NAME_ADD)) {
            ModelManager modelManager = appContext.getConsoleContext().getModelManager();
            ModelBase modelInstance = null;//TODO modelManager.getModelInstance(request.getModelName());
            if (modelInstance==null) {
                return null;
            }
            RequestImpl requestTemp = ((RequestImpl) request).clone();
            if (StringUtil.isBlank(requestTemp.getParameter(ListModel.FIELD_NAME_ID))) {
                requestTemp.setId(modelInstance.resolveId(requestTemp));
            }
            Response responseTemp = new qingzhou.framework.app.ResponseImpl();
            modelInstance.show(requestTemp, responseTemp);
            boolean exists = responseTemp.isSuccess() && !responseTemp.getDataList().isEmpty();
            if (exists) {
                String modelNameI18n = appContext.getMetadata().getI18n(request.getI18nLang(), "model." + request.getModelName());
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
