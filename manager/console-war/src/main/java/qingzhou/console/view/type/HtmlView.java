package qingzhou.console.view.type;

import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.view.View;
import qingzhou.deployer.ActionInvoker;
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
        HttpServletRequest req = restContext.req;

        boolean isManageAction = isManageAction(request);
        if (isManageAction) {
            request.setAppName(request.getId());
            request.setModelName(DeployerConstants.MODEL_HOME);
            request.setActionName(DeployerConstants.ACTION_SHOW);
            Response response = SystemController.getService(ActionInvoker.class).invokeOnce(request);
            request.setResponse(response);
        }
        req.setAttribute(Request.class.getName(), request);

        String pageForward = isManageAction ? "sys/manage" : getPageForward(request);
        String forwardToPage = HtmlView.htmlPageBase + (pageForward.contains("/") ? (pageForward + ".jsp") : ("view/" + pageForward + ".jsp"));
        restContext.req.getRequestDispatcher(forwardToPage).forward(restContext.req, restContext.resp);
    }

    private boolean isManageAction(Request request) {
        if (!DeployerConstants.ACTION_MANAGE.equals(request.getAction())) return false;
        if (!DeployerConstants.APP_SYSTEM.equals(request.getApp())) return false;

        return DeployerConstants.MODEL_APP.equals(request.getModel())
                || DeployerConstants.MODEL_INSTANCE.equals(request.getModel());
    }

    @Override
    public String getContentType() {
        return "text/html;charset=UTF-8";
    }

    private String getPageForward(Request request) {
        if ((DeployerConstants.MODEL_INDEX.equals(request.getModel())
                || DeployerConstants.MODEL_HOME.equals(request.getModel()))
                && request.getAction().equals(DeployerConstants.ACTION_SHOW)) {
            return "home";
        }

        ModelActionInfo actionInfo = SystemController.getAppInfo(request.getApp()).getModelInfo(request.getModel()).getModelActionInfo(request.getAction());
        if (Utils.notBlank(actionInfo.getPage())) {
            return actionInfo.getPage();
        }

        switch (request.getAction()) {
            case DeployerConstants.ACTION_LIST:
            case DeployerConstants.ACTION_DELETE:
                return "list";
            case DeployerConstants.ACTION_CREATE:
            case DeployerConstants.ACTION_EDIT:
                return "form";
            case DeployerConstants.ACTION_SHOW:
            case DeployerConstants.ACTION_MONITOR:
                return "show";
            case DeployerConstants.ACTION_INDEX:
                return "sys/index";
            case DeployerConstants.ACTION_MANAGE:
                return "sys/manage";
        }

        return "default";
    }
}
