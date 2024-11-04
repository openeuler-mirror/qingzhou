package qingzhou.console.view.type;

import qingzhou.api.ActionType;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.api.type.*;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.view.View;
import qingzhou.deployer.ActionInvoker;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.RequestImpl;

import javax.servlet.http.HttpServletRequest;

public class HtmlView implements View {
    public static final String HTML_PAGE_BASE = "/WEB-INF/view/";
    public static final String FLAG = "html";

    @Override
    public void render(RestContext restContext) throws Exception {
        RequestImpl request = restContext.request;
        HttpServletRequest servletRequest = restContext.req;

        servletRequest.setAttribute(Request.class.getName(), request);
        boolean isManageAction = isManageApp(request);
        if (isManageAction) {
            request.setAppName(request.getId());
            request.setModelName("home"); // qingzhou.app.common.Home 的 code
            request.setCachedModelInfo(SystemController.getModelInfo(request.getApp(), request.getModel())); // 重新缓存
            request.setActionName(Show.ACTION_SHOW);
            Response response = SystemController.getService(ActionInvoker.class).invokeSingle(request);
            request.setResponse(response);// 用远端的请求替换本地的，如果是本地实例，它俩是等效的
        }

        String forwardView = isManageAction ? "sys/manage" : getForwardView(request);
        String forwardToPage = HtmlView.HTML_PAGE_BASE + (forwardView.contains("/") ? (forwardView + ".jsp") : ("type/" + forwardView + ".jsp"));
        restContext.req.getRequestDispatcher(forwardToPage).forward(restContext.req, restContext.resp);
    }

    private boolean isManageApp(Request request) {
        if (!DeployerConstants.ACTION_MANAGE.equals(request.getAction())) return false;
        if (!DeployerConstants.APP_SYSTEM.equals(request.getApp())) return false;
        return DeployerConstants.MODEL_APP.equals(request.getModel());
    }

    @Override
    public String getContentType() {
        return "text/html;charset=UTF-8";
    }

    private String getForwardView(RequestImpl request) {
        String actionName = request.getAction();
        switch (actionName) {
            case Show.ACTION_SHOW:
                return "show";
            case Add.ACTION_CREATE:
            case Update.ACTION_EDIT:
                return "form";
            case List.ACTION_LIST:
                return "list";
            case Monitor.ACTION_MONITOR:
                return "monitor";
            case Chart.ACTION_CHART:
                return "chart";
            case DeployerConstants.ACTION_INDEX:
                return "sys/index";
            case DeployerConstants.ACTION_MANAGE:
                return "sys/manage";
            case Combined.ACTION_COMBINED:
                return "combined";
        }

        ActionType actionType = request.getCachedModelInfo().getModelActionInfo(actionName).getActionType();
        if (actionType == ActionType.sub_menu) {
            return "sys/sub_inddex";
        }

        return "default";
    }
}
