package qingzhou.console.view.impl;

import qingzhou.console.ConsoleConstants;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.console.page.PageBackendService;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.RequestImpl;
import qingzhou.framework.api.ModelAction;
import qingzhou.framework.api.Response;
import qingzhou.framework.api.ShowModel;

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
        String pageForward;
        if (modelAction != null) {
            pageForward = modelAction.forwardToPage();
        } else {
            pageForward = "default";
        }

        if (FrameworkContext.SYS_ACTION_MANAGE.equals(actionName)) {
            String appName = request.getId();
            String targetModelName = null;
            String targetModelAction = null;
            if (ConsoleConstants.MODEL_NAME_app.equals(modelName)) {
                request.setManageType(ConsoleConstants.MANAGE_TYPE_APP);
                targetModelName = ConsoleConstants.MODEL_NAME_apphome;
                targetModelAction = ShowModel.ACTION_NAME_SHOW;
            } else if (ConsoleConstants.MODEL_NAME_node.equals(modelName)) {
                request.setManageType(ConsoleConstants.MANAGE_TYPE_NODE);
                appName = FrameworkContext.SYS_NODE_LOCAL;
                targetModelName = ConsoleConstants.MODEL_NAME_home;
                targetModelAction = ShowModel.ACTION_NAME_SHOW;
            }
            request.setAppName(appName);
            request.setModelName(targetModelName);
            request.setActionName(targetModelAction);
            if(FrameworkContext.SYS_NODE_LOCAL.equals(appName)){
                appName = FrameworkContext.SYS_APP_NODE_AGENT;
            }
            ConsoleWarHelper.getAppManager().getApp(appName).invoke(request, response);
        }

        String forwardToPage = HtmlView.htmlPageBase + "view/" + pageForward + ".jsp";
        restContext.servletRequest.getRequestDispatcher(forwardToPage).forward(restContext.servletRequest, restContext.servletResponse);
    }

    @Override
    public String getContentType() {
        return "text/html;charset=UTF-8";
    }
}
