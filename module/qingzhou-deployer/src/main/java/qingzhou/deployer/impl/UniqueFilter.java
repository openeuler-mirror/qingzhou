package qingzhou.deployer.impl;

import qingzhou.api.ActionFilter;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.api.type.Createable;
import qingzhou.deployer.ResponseImpl;

class UniqueFilter implements ActionFilter {
    private final I18nTool i18nTool = new I18nTool();
    private final AppContextImpl appContext;
    private volatile boolean i18nDone = false;

    UniqueFilter(AppContextImpl appContext) {
        this.appContext = appContext;
    }

    @Override
    public String doFilter(Request request, Response response) throws Exception {
        initI18n();

        if (request.getAction().equals(Createable.ACTION_NAME_ADD)) {
            // NOTE：能进入 filter 里面，已经经过了 AppInfoImpl.invoke 的 校验， model action 都应是合法的，不必重复校验
            ResponseImpl tempResponse = new ResponseImpl();

            if (tempResponse.isSuccess() && !tempResponse.getDataList().isEmpty()) {
                String modelNameI18n = appContext.getI18n(request.getLang(), "model." + request.getModel());
                return i18nTool.getI18n(request.getLang(), "list.id.exists", modelNameI18n);
            }
        }

        return null;
    }

    private void initI18n() {
        if (i18nDone) return;

        synchronized (UniqueFilter.class) {
            if (i18nDone) return;
            i18nDone = true;

            i18nTool.addI18n("list.id.exists", new String[]{"%s已存在", "en:%s already exists"});
        }
    }
}
