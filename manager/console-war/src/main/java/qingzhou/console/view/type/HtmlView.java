package qingzhou.console.view.type;

import qingzhou.api.Request;
import qingzhou.console.ActionInvoker;
import qingzhou.console.ConsoleConstants;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.view.View;
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
                request.setManageType("app");
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
        if (!ConsoleConstants.ACTION_NAME_manage.equals(request.getAction())) return false;

        if (!"master".equals(request.getApp())) return false;

        return "app".equals(request.getModel())
                || "instance".equals(request.getModel());
    }

    @Override
    public String getContentType() {
        return "text/html;charset=UTF-8";
    }

    private String getPageForward(Request request, boolean isManageAction) {
        if (isManageAction) {
            return ConsoleConstants.VIEW_RENDER_MANAGE;
        }

        if ((ConsoleConstants.MODEL_NAME_index.equals(request.getModel()) || ConsoleConstants.MODEL_NAME_apphome.equals(request.getModel()))
                && ConsoleConstants.ACTION_NAME_SHOW.equals(request.getAction())) {
            return ConsoleConstants.VIEW_RENDER_HOME;
        }

        ModelActionInfo actionInfo = SystemController.getAppInfo(request.getApp()).getModelInfo(request.getModel()).getModelActionInfo(request.getAction());
        if (Utils.notBlank(actionInfo.getPage())) {
            return actionInfo.getPage();
        }

        switch (request.getAction()) {
            case "list":
                return ConsoleConstants.VIEW_RENDER_LIST;
            case "create":
            case "edit":
                return ConsoleConstants.VIEW_RENDER_FORM;
            case "index":
                return ConsoleConstants.VIEW_RENDER_INDEX;
            case ConsoleConstants.ACTION_NAME_manage:
                return ConsoleConstants.VIEW_RENDER_MANAGE;
            case ConsoleConstants.ACTION_NAME_SHOW:
            case "monitor":
                return ConsoleConstants.VIEW_RENDER_SHOW;
        }

        return ConsoleConstants.VIEW_RENDER_DEFAULT;
    }
}
