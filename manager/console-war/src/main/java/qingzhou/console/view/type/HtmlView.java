package qingzhou.console.view.type;

import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.console.ConsoleConstants;
import qingzhou.console.RequestImpl;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.view.View;

import javax.servlet.http.HttpServletRequest;

public class HtmlView implements View {
    public static final String QZ_REQUEST_KEY = "QZ_REQUEST_KEY";
    public static final String QZ_RESPONSE_KEY = "QZ_RESPONSE_KEY";
    public static final String htmlPageBase = "/WEB-INF/page/";

    @Override
    public void render(RestContext restContext) throws Exception {
        RequestImpl request = restContext.request;
        Response response = restContext.response;
        HttpServletRequest req = restContext.servletRequest;
        req.setAttribute(QZ_REQUEST_KEY, request);
        req.setAttribute(QZ_RESPONSE_KEY, response);

        String modelName = request.getModel();
        String actionName = request.getAction();
        ModelActionData modelAction = SystemController.getAppMetadata(request).getModelManager().getModelAction(modelName, actionName);
        String pageForward = null;
        if (modelAction != null) {
            pageForward = modelAction.forwardToPage();
        }
        if (StringUtil.isBlank(pageForward)) {
            pageForward = "default";
        }

        if (isManageAction(request)) {
            String manageAppName = null;
            if ("app".equals(modelName)) {
                request.setManageType(ConsoleConstants.MANAGE_TYPE_APP);
                manageAppName = request.getId();
            } else if ("node".equals(modelName)) {
                request.setManageType(ConsoleConstants.MANAGE_TYPE_NODE);
                manageAppName = "local";
            }
            request.setAppName(manageAppName);
            request.setModelName("home");
            request.setActionName("show");
            SystemController.invokeLocalApp(request, response);// todo：到 html render 这里已经执行过一次 invoke app 了，这里是重复执行可以优化?
        }

        String forwardToPage = HtmlView.htmlPageBase + (pageForward.contains("/") ? (pageForward + ".jsp") : ("view/" + pageForward + ".jsp"));
        restContext.servletRequest.getRequestDispatcher(forwardToPage).forward(restContext.servletRequest, restContext.servletResponse);
    }

    private boolean isManageAction(Request request) {
        if (!"manage".equals(request.getAction())) return false;

        if (!"master".equals(request.getApp())) return false;

        return "app".equals(request.getModel())
                || "node".equals(request.getModel());
    }

    @Override
    public String getContentType() {
        return "text/html;charset=UTF-8";
    }
}
