package qingzhou.app.impl;

import qingzhou.api.ActionFilter;
import qingzhou.api.AppContext;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.api.type.Createable;
import qingzhou.console.I18nTool;
import qingzhou.console.ResponseImpl;

public class UniqueFilter implements ActionFilter {
    private final I18nTool i18nTool = new I18nTool();
    private volatile boolean i18nDone = false;

    @Override
    public String doFilter(Request request, Response response, AppContext appContext) throws Exception {
        initI18n();

        if (request.getActionName().equals(Createable.ACTION_NAME_ADD)) {
            // NOTE：能进入 filter 里面，已经经过了 qingzhou.app.AppInfoImpl.invoke 的 校验， model action 都应是合法的，不必重复校验
            ResponseImpl tempResponse = new ResponseImpl();

            if (tempResponse.isSuccess() && !tempResponse.getDataList().isEmpty()) {
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
