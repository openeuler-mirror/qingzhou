package qingzhou.console.view.type;

import qingzhou.api.Request;
import qingzhou.console.ActionInvoker;
import qingzhou.console.ConsoleConstants;
import qingzhou.console.RequestImpl;
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
        HttpServletRequest req = restContext.servletRequest;

        String modelName = request.getModel();
        boolean isManageAction = isManageAction(request);
        if (isManageAction) {
            String manageAppName = null;
            if ("app".equals(modelName)) {
                request.setManageType(ConsoleConstants.MANAGE_TYPE_APP);
                manageAppName = request.getId();
            } else if ("instance".equals(modelName)) {
                request.setManageType(ConsoleConstants.MANAGE_TYPE_INSTANCE);
                manageAppName = "local";
            }
            request.setAppName(manageAppName);
            request.setModelName("home");
            request.setActionName("show");
            restContext.response = ActionInvoker.getInstance().invokeAction(request);
        }
        req.setAttribute(QZ_REQUEST_KEY, request);
        req.setAttribute(QZ_RESPONSE_KEY, restContext.response);

        String pageForward = getPageForward(request.getModel(), request.getAction(), isManageAction);
        String forwardToPage = HtmlView.htmlPageBase + (pageForward.contains("/") ? (pageForward + ".jsp") : ("view/" + pageForward + ".jsp"));
        restContext.servletRequest.getRequestDispatcher(forwardToPage).forward(restContext.servletRequest, restContext.servletResponse);
    }

    private boolean isManageAction(Request request) {
        if (!"manage".equals(request.getAction())) return false;

        if (!"master".equals(request.getApp())) return false;

        return "app".equals(request.getModel())
                || "instance".equals(request.getModel());
    }

    @Override
    public String getContentType() {
        return "text/html;charset=UTF-8";
    }

    private String getPageForward(String model, String action, boolean isManageAction) {
        if (isManageAction) {
            return ConsoleConstants.VIEW_RENDER_MANAGE;
        }

        if ((ConsoleConstants.MODEL_NAME_index.equals(model) || ConsoleConstants.MODEL_NAME_apphome.equals(model))
                && ConsoleConstants.ACTION_NAME_show.equals(action)) {
            return ConsoleConstants.VIEW_RENDER_HOME;
        }

        switch (action) {
            case ConsoleConstants.ACTION_NAME_list:
                return ConsoleConstants.VIEW_RENDER_LIST;
            case ConsoleConstants.ACTION_NAME_create:
            case ConsoleConstants.ACTION_NAME_edit:
                return ConsoleConstants.VIEW_RENDER_FORM;
            case ConsoleConstants.ACTION_NAME_index:
                return ConsoleConstants.VIEW_RENDER_INDEX;
            case ConsoleConstants.ACTION_NAME_monitor:
                return ConsoleConstants.VIEW_RENDER_INFO;
            case ConsoleConstants.ACTION_NAME_manage:
                return ConsoleConstants.VIEW_RENDER_MANAGE;
        }
        return ConsoleConstants.VIEW_RENDER_DEFAULT;
    }
}
