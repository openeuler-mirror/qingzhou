package qingzhou.console.view.impl;

import qingzhou.console.ConsoleUtil;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.framework.api.ModelAction;
import qingzhou.framework.api.Response;
import qingzhou.framework.api.ShowModel;
import qingzhou.framework.console.RequestImpl;
import qingzhou.framework.console.ConsoleConstants;

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
        ModelAction modelAction = ConsoleUtil.getModelManager(request.getAppName()).getModelAction(modelName, actionName);
        String pageForward;
        if (modelAction != null) {
            pageForward = modelAction.forwardToPage();
        } else {
            pageForward = "default";
        }


        if (ConsoleUtil.ACTION_NAME_TARGET.equals(actionName)) {
            if (ConsoleConstants.MODEL_NAME_cluster.equals(modelName) || ConsoleConstants.MODEL_NAME_node.equals(modelName)) {
                request.setModelName(ConsoleConstants.MODEL_NAME_home);
                request.setActionName(ShowModel.ACTION_NAME_SHOW);
            }
        }

        String forwardToPage = HtmlView.htmlPageBase + "view/" + pageForward + ".jsp";
        restContext.servletRequest.getRequestDispatcher(forwardToPage).forward(restContext.servletRequest, restContext.servletResponse);
    }

    @Override
    public String getContentType() {
        return "text/html;charset=UTF-8";
    }
}
