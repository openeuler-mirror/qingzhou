package qingzhou.console.controller.rest;

import qingzhou.api.Response;
import qingzhou.console.SecurityController;
import qingzhou.console.controller.I18n;
import qingzhou.console.controller.SystemController;
import qingzhou.deployer.RequestImpl;
import qingzhou.engine.util.pattern.Filter;
import qingzhou.registry.AppInfo;
import qingzhou.registry.ModelActionInfo;
import qingzhou.registry.ModelInfo;

public class ActionFilter implements Filter<RestContext> {
    @Override
    public boolean doFilter(RestContext context) throws Exception {
        RequestImpl request = context.request;

        Response response = request.getResponse();
        AppInfo appInfo = SystemController.getAppInfo(SystemController.getAppName(request));
        ModelInfo modelInfo = appInfo.getModelInfo(request.getModel());

        // 拦截禁止删除等操作（如禁止通过 rest 接口删除 qingzhou  默认账户）
        ModelActionInfo actionInfo = modelInfo.getModelActionInfo(request.getAction());
        for (String id : context.batchIds) {
            request.setParameter(modelInfo.getIdFieldName(), id);
            if (!SecurityController.isShow(actionInfo.getShow(), request::getParameter)) {
                String i18n = I18n.getKeyI18n("validation_action", actionInfo.getCode(), actionInfo.getShow());
                response.setSuccess(false);
                response.setMsg(i18n);
                return false;
            }
        }
        request.removeParameter(modelInfo.getIdFieldName());

        return true;
    }
}
