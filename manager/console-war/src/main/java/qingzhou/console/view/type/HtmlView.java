package qingzhou.console.view.type;

import qingzhou.api.Request;
import qingzhou.api.type.Createable;
import qingzhou.api.type.Editable;
import qingzhou.api.type.Listable;
import qingzhou.api.type.Monitorable;
import qingzhou.api.type.Showable;
import qingzhou.console.ActionInvoker;
import qingzhou.console.ConsoleConstants;
import qingzhou.console.RequestImpl;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.view.View;
import qingzhou.deployer.DeployerConstants;

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
            if (DeployerConstants.MASTER_APP_APP_MODEL_NAME.equals(modelName)) {
                request.setManageType(DeployerConstants.MANAGE_TYPE_APP);
            } else if (DeployerConstants.MASTER_APP_INSTANCE_MODEL_NAME.equals(modelName)) {
                request.setManageType(DeployerConstants.MANAGE_TYPE_INSTANCE);
            }
            request.setAppName(request.getId());
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
        if (!ConsoleConstants.ACTION_NAME_manage.equals(request.getAction())) return false;

        if (!DeployerConstants.MASTER_APP_NAME.equals(request.getApp())) return false;

        return DeployerConstants.MASTER_APP_APP_MODEL_NAME.equals(request.getModel())
                || DeployerConstants.MASTER_APP_INSTANCE_MODEL_NAME.equals(request.getModel());
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
                && Showable.ACTION_NAME_SHOW.equals(action)) {
            return ConsoleConstants.VIEW_RENDER_HOME;
        }

        switch (action) {
            case Listable.ACTION_NAME_LIST:
                return ConsoleConstants.VIEW_RENDER_LIST;
            case Createable.ACTION_NAME_CREATE:
            case Editable.ACTION_NAME_EDIT:
                return ConsoleConstants.VIEW_RENDER_FORM;
            case ConsoleConstants.ACTION_NAME_index:
                return ConsoleConstants.VIEW_RENDER_INDEX;
            case ConsoleConstants.ACTION_NAME_manage:
                return ConsoleConstants.VIEW_RENDER_MANAGE;
            case Showable.ACTION_NAME_SHOW:
            case Monitorable.ACTION_NAME_MONITOR:
                return ConsoleConstants.VIEW_RENDER_SHOW;
        }
        return ConsoleConstants.VIEW_RENDER_DEFAULT;
    }
}
