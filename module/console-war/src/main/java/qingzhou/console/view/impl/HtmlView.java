package qingzhou.console.view.impl;

import qingzhou.console.ConsoleUtil;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.page.PageBackendService;
import qingzhou.framework.api.ModelAction;
import qingzhou.framework.api.ModelManager;
import qingzhou.framework.api.Response;
import qingzhou.framework.api.ShowModel;
import qingzhou.framework.console.ConsoleConstants;
import qingzhou.framework.console.RequestImpl;

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


        if (ConsoleUtil.ACTION_NAME_TARGET.equals(actionName)) {
            if (ConsoleConstants.MODEL_NAME_node.equals(modelName)) { // todo 这个还需要？
                request.setAppName(ConsoleConstants.NODE_APP_NAME);
                request.setModelName(ConsoleConstants.MODEL_NAME_home);
                request.setActionName(ShowModel.ACTION_NAME_SHOW);
            } else if (ConsoleConstants.MODEL_NAME_app.equals(modelName)) {// todo 是不是只有 app 会有 target action？ target设计是否有问题?
                String appName = request.getId();
                ModelManager modelManager = PageBackendService.getModelManager(appName);
                if (modelManager != null) {
                    String[] modelNames = modelManager.getModelNames();// TODO 应用管理是否需要配置一个默认展示model？
                    String targetModelName = "";
                    if (modelNames.length > 0) {
                        targetModelName = modelNames[0];
                    }
                    request.setAppName(request.getId());
                    request.setModelName(targetModelName);
                    request.setActionName(modelManager.getModel(targetModelName).entryAction());
                }
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
