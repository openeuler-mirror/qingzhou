package qingzhou.console.view.type;

import javax.servlet.http.HttpServletRequest;

import qingzhou.api.ActionType;
import qingzhou.api.AppContext;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.api.type.Add;
import qingzhou.api.type.Chart;
import qingzhou.api.type.Combined;
import qingzhou.api.type.Dashboard;
import qingzhou.api.type.List;
import qingzhou.api.type.Monitor;
import qingzhou.api.type.Show;
import qingzhou.api.type.Update;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.view.View;
import qingzhou.core.DeployerConstants;
import qingzhou.core.deployer.ActionInvoker;
import qingzhou.core.deployer.RequestImpl;

public class HtmlView implements View {
    public static final String HTML_PAGE_BASE = "/WEB-INF/view/";
    public static final String FLAG = "html";

    @Override
    public void render(RestContext restContext) throws Exception {
        RequestImpl request = restContext.request;
        HttpServletRequest servletRequest = restContext.req;

        servletRequest.setAttribute(Request.class.getName(), request);
        if (request.getParameter(DeployerConstants.RETURNS_LINK_PARAM_NAME_RETURNSID) != null && !"".equals(request.getParameter(DeployerConstants.RETURNS_LINK_PARAM_NAME_RETURNSID))) {
            servletRequest.setAttribute(DeployerConstants.RETURNS_LINK_PARAM_NAME_RETURNSID, request.getParameter(DeployerConstants.RETURNS_LINK_PARAM_NAME_RETURNSID));
        }
        boolean isManageAction = isManageApp(request);
        if (isManageAction) {
            switchToManagedAction(request);
        }

        String forwardView = isManageAction ? "sys/manage" : getForwardView(request);
        String forwardToPage = HtmlView.HTML_PAGE_BASE + (forwardView.contains("/") ? (forwardView + ".jsp") : ("type/" + forwardView + ".jsp"));
        restContext.req.getRequestDispatcher(forwardToPage).forward(restContext.req, restContext.resp);
    }

    private boolean isManageApp(Request request) {
        if (!DeployerConstants.ACTION_MANAGE.equals(request.getAction())) return false;
        if (!DeployerConstants.APP_MASTER.equals(request.getApp())) return false;
        return DeployerConstants.MODEL_APP.equals(request.getModel());
    }

    private void switchToManagedAction(RequestImpl request) {
        request.setAppName(request.getId());
        request.setModelName(AppContext.APP_HOME_MODEL); // qingzhou.app.common.Home 的 code
        request.setActionName(Show.ACTION_SHOW);
        request.setCachedModelInfo(SystemController.getModelInfo(request.getId(), AppContext.APP_HOME_MODEL)); // 重新缓存
        Response response = SystemController.getService(ActionInvoker.class).invokeAny(request);
        request.setResponse(response);// 用远端的请求替换本地的，如果是本地实例，它俩是等效的
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
            case Combined.ACTION_COMBINED:
                return "combined";
            case Dashboard.ACTION_DASHBOARD:
                return "dashboard";
            case DeployerConstants.ACTION_INDEX:
                return "sys/index";
            case DeployerConstants.ACTION_MANAGE:
                return "sys/manage";
        }

        ActionType actionType = request.getCachedModelInfo().getModelActionInfo(actionName).getActionType();
        if (actionType == ActionType.sub_menu) {
            return "sys/sub_index";
        }

        return "default";
    }
}
