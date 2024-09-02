package qingzhou.console.view.type;

import qingzhou.api.Request;
import qingzhou.console.ActionInvoker;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.view.View;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.RequestImpl;
import qingzhou.engine.util.Utils;
import qingzhou.registry.ModelActionInfo;

import javax.servlet.http.HttpServletRequest;

public class HtmlView implements View {
    public static final String htmlPageBase = "/WEB-INF/page/";

    @Override
    public void render(RestContext restContext) throws Exception {
        RequestImpl request = restContext.request;
        HttpServletRequest req = restContext.servletRequest;

        String modelName = request.getModel();
        boolean isManageAction = isManageAction(request);
        if (isManageAction) {
            if (DeployerConstants.APP_MODEL.equals(modelName)) {
                request.setManageType(DeployerConstants.APP_MANAGE);
            } else if (DeployerConstants.INSTANCE_MODEL.equals(modelName)) {
                request.setManageType(DeployerConstants.INSTANCE_MANAGE);
            }
            request.setAppName(request.getId());
            request.setModelName(DeployerConstants.HOME_MODEL);
            request.setActionName(DeployerConstants.SHOW_ACTION);
            ActionInvoker.getInstance().invokeAction(request);
        }
        req.setAttribute(Request.class.getName(), request);

        String pageForward = getPageForward(request, isManageAction);
        String forwardToPage = HtmlView.htmlPageBase + (pageForward.contains("/") ? (pageForward + ".jsp") : ("view/" + pageForward + ".jsp"));
        restContext.servletRequest.getRequestDispatcher(forwardToPage).forward(restContext.servletRequest, restContext.servletResponse);
    }

    private boolean isManageAction(Request request) {
        if (!DeployerConstants.MANAGE_ACTION.equals(request.getAction())) return false;
        if (!DeployerConstants.MASTER_APP.equals(request.getApp())) return false;

        return DeployerConstants.APP_MODEL.equals(request.getModel())
                || DeployerConstants.INSTANCE_MODEL.equals(request.getModel());
    }

    @Override
    public String getContentType() {
        return "text/html;charset=UTF-8";
    }

    private String getPageForward(Request request, boolean isManageAction) {
        if (isManageAction) {
            return "sys/manage";
        }

        if ((DeployerConstants.INDEX_MODEL.equals(request.getModel())
                || DeployerConstants.HOME_MODEL.equals(request.getModel()))
                && request.getAction().equals(DeployerConstants.SHOW_ACTION)) {
            return "home";
        }

        ModelActionInfo actionInfo = SystemController.getAppInfo(request.getApp()).getModelInfo(request.getModel()).getModelActionInfo(request.getAction());
        if (Utils.notBlank(actionInfo.getPage())) {
            return actionInfo.getPage();
        }

        switch (request.getAction()) {
            case DeployerConstants.LIST_ACTION:
            case DeployerConstants.DELETE_ACTION:
                return "list";
            case DeployerConstants.CREATE_ACTION:
            case DeployerConstants.EDIT_ACTION:
                return "form";
            case DeployerConstants.SHOW_ACTION:
            case DeployerConstants.MONITOR_ACTION:
                return "show";
            case DeployerConstants.INDEX_ACTION:
                return "sys/index";
            case DeployerConstants.MANAGE_ACTION:
                return "sys/manage";
        }

        return "default";
    }
}
