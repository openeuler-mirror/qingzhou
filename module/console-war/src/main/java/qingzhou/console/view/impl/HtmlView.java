package qingzhou.console.view.impl;

import qingzhou.console.ConsoleConstants;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.console.page.PageBackendService;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.app.RequestImpl;
import qingzhou.framework.api.ModelAction;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;
import qingzhou.framework.api.ShowModel;
import qingzhou.framework.util.StringUtil;

import javax.servlet.http.HttpServletRequest;

public class HtmlView implements View {
    public static final String QZ_REQUEST_KEY = "QZ_REQUEST_KEY";
    public static final String QZ_RESPONSE_KEY = "QZ_RESPONSE_KEY";
    public static final String htmlPageBase = "/WEB-INF/page/";

    @Override
    public void render(RestContext restContext) throws Exception {
        RequestImpl request = (RequestImpl) restContext.request;
        Response response = restContext.response;
        // 将request,response放入HttpServletRequest，以供 jsp 使用，如果json也要使用，则应该将此处的代码移动到 ViewManager 总入口链里面
        HttpServletRequest req = restContext.servletRequest;
        req.setAttribute(QZ_REQUEST_KEY, request);
        req.setAttribute(QZ_RESPONSE_KEY, response);

        String modelName = request.getModelName();
        String actionName = request.getActionName();
        ModelAction modelAction = PageBackendService.getModelManager(request.getAppName()).getModelAction(modelName, actionName);
        String pageForward = null;
        if (modelAction != null) {
            pageForward = modelAction.forwardToPage();
        }
        if (StringUtil.isBlank(pageForward)) {
            pageForward = "default";
        }

        if (isManageAction(request)) {
            String manageAppName = request.getId();
            if (FrameworkContext.SYS_MODEL_APP.equals(modelName)) {
                request.setManageType(FrameworkContext.MANAGE_TYPE_APP);
            } else if (ConsoleConstants.MODEL_NAME_node.equals(modelName)) {
                request.setManageType(FrameworkContext.MANAGE_TYPE_NODE);
                manageAppName = FrameworkContext.SYS_NODE_LOCAL;
            }
            request.setAppName(manageAppName);
            request.setModelName(FrameworkContext.SYS_MODEL_HOME);
            request.setActionName(ShowModel.ACTION_NAME_SHOW);
            if (FrameworkContext.SYS_NODE_LOCAL.equals(manageAppName)) {
                manageAppName = FrameworkContext.SYS_APP_NODE_AGENT;
            }
            ConsoleWarHelper.invokeLocalApp(manageAppName, request, response);// todo 对于远程的，这里数据不对?
        }

        String forwardToPage = HtmlView.htmlPageBase + (pageForward.contains("/") ? (pageForward + ".jsp") : ("view/" + pageForward + ".jsp"));
        restContext.servletRequest.getRequestDispatcher(forwardToPage).forward(restContext.servletRequest, restContext.servletResponse);
    }

    private boolean isManageAction(Request request) {
        if (!FrameworkContext.SYS_ACTION_MANAGE.equals(request.getActionName())) return false;

        if (!FrameworkContext.SYS_APP_MASTER.equals(request.getAppName())) return false;

        return FrameworkContext.SYS_MODEL_APP.equals(request.getModelName())
                || FrameworkContext.SYS_MODEL_NODE.equals(request.getModelName());
    }

    @Override
    public String getContentType() {
        return "text/html;charset=UTF-8";
    }
}
