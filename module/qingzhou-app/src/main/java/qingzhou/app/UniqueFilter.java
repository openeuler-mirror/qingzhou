package qingzhou.app;

import qingzhou.api.*;
import qingzhou.api.type.Createable;
import qingzhou.api.type.Listable;
import qingzhou.framework.console.I18nTool;
import qingzhou.framework.console.RequestImpl;
import qingzhou.framework.util.StringUtil;

public class UniqueFilter implements ActionFilter {
    private final I18nTool i18nTool = new I18nTool();
    private volatile boolean i18nDone = false;

    @Override
    public String doFilter(Request request, Response response, AppContext appContext) throws Exception {
        initI18n();

        if (request.getActionName().equals(Createable.ACTION_NAME_ADD)) {
            /*ModelBase modelInstance = null;//TODO modelManager.getModelInstance();
            RequestImpl requestTemp = ((RequestImpl) request).clone();
            if (StringUtil.isBlank(requestTemp.getParameter(Listable.FIELD_NAME_ID))) {
                requestTemp.setId(modelInstance.resolveId(requestTemp));
            }*/
            if (appContext.getDefaultDataStore().exists(request.getModelName(), request.getId())) {
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
