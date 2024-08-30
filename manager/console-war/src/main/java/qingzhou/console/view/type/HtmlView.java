package qingzhou.console.view.type;

import qingzhou.api.Request;
import qingzhou.console.ActionInvoker;
import qingzhou.console.ConsoleConstants;
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
            if ("app".equals(modelName)) {
                request.setManageType(DeployerConstants.APP_MANAGE);
            } else if ("instance".equals(modelName)) {
                request.setManageType("instance");
            }
            request.setAppName(request.getId());
            request.setModelName("home");
            request.setActionName("show");
            ActionInvoker.getInstance().invokeAction(request);
        }
        req.setAttribute(Request.class.getName(), request);

        String pageForward = getPageForward(request, isManageAction);
        String forwardToPage = HtmlView.htmlPageBase + (pageForward.contains("/") ? (pageForward + ".jsp") : ("view/" + pageForward + ".jsp"));
        restContext.servletRequest.getRequestDispatcher(forwardToPage).forward(restContext.servletRequest, restContext.servletResponse);
    }

    private boolean isManageAction(Request request) {
        if (!"manage".equals(request.getAction())) return false;
        if (!DeployerConstants.MASTER_APP.equals(request.getApp())) return false;

        return "app".equals(request.getModel())
                || "instance".equals(request.getModel());
    }

    @Override
    public String getContentType() {
        return "text/html;charset=UTF-8";
    }

    private String getPageForward(Request request, boolean isManageAction) {
        if (isManageAction) {
            return "sys/manage";
        }

        if ((ConsoleConstants.MODEL_NAME_index.equals(request.getModel()) || ConsoleConstants.MODEL_NAME_apphome.equals(request.getModel()))
                && request.getAction().equals("show")) {
            return "home";
        }

        ModelActionInfo actionInfo = SystemController.getAppInfo(request.getApp()).getModelInfo(request.getModel()).getModelActionInfo(request.getAction());
        if (Utils.notBlank(actionInfo.getPage())) {
            return actionInfo.getPage();
        }

        switch (request.getAction()) {
            case "list":
            case "delete":
                return "list";
            case "create":
            case DeployerConstants.EDIT_ACTION:
                return "form";
            case "show":
            case "monitor":
                return "show";
            case "index":
                return "sys/index";
            case "manage":
                return "sys/manage";
        }

        return "default";
    }
}
