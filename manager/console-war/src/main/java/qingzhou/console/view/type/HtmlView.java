package qingzhou.console.view.type;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.api.type.Show;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.view.View;
import qingzhou.deployer.ActionInvoker;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.RequestImpl;
import qingzhou.engine.util.Utils;
import qingzhou.registry.ModelActionInfo;

public class HtmlView implements View {
    public static final String htmlPageBase = "/WEB-INF/page/";

    @Override
    public void render(RestContext restContext) throws Exception {
        HttpServletResponse resp = restContext.resp;
        if (resp.isCommitted()) return;

        RequestImpl request = restContext.request;
        HttpServletRequest req = restContext.req;

        boolean isManageAction = isManageAction(request);
        if (isManageAction) {
            request.setAppName(request.getId());
            request.setModelName("home"); // qingzhou.app.common.Home 的 code
            request.setCachedModelInfo(SystemController.getModelInfo(request.getApp(), request.getModel())); // 重新缓存
            request.setActionName(Show.ACTION_SHOW);
            Response response = SystemController.getService(ActionInvoker.class).invokeSingle(request);
            request.setResponse(response);
        }
        req.setAttribute(Request.class.getName(), request);

        String pageForward = isManageAction ? "sys/manage" : getPageForward(request);
        String forwardToPage = HtmlView.htmlPageBase + (pageForward.contains("/") ? (pageForward + ".jsp") : ("view/" + pageForward + ".jsp"));
        restContext.req.getRequestDispatcher(forwardToPage).forward(restContext.req, restContext.resp);
    }

    private boolean isManageAction(Request request) {
        if (!DeployerConstants.ACTION_MANAGE.equals(request.getAction())) return false;
        if (!DeployerConstants.APP_SYSTEM.equals(request.getApp())) return false;
        return DeployerConstants.MODEL_APP.equals(request.getModel());
    }

    @Override
    public String getContentType() {
        return "text/html;charset=UTF-8";
    }

    private String getPageForward(RequestImpl request) {
        ModelActionInfo actionInfo = request.getCachedModelInfo().getModelActionInfo(request.getAction());
        if (Utils.notBlank(actionInfo.getPage())) {
            return actionInfo.getPage();
        }

        return "default";
    }
}
