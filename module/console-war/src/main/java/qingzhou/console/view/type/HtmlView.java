package qingzhou.console.view.type;

import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.api.type.Showable;
import qingzhou.console.ConsoleConstants;
import qingzhou.console.RestContext;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.console.view.View;
import qingzhou.framework.app.App;
import qingzhou.framework.app.RequestImpl;
import qingzhou.framework.util.StringUtil;
import qingzhou.framework.app.ModelActionData;

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
        ModelActionData modelAction = ConsoleWarHelper.getAppStub(request.getAppName()).getModelManager().getModelAction(modelName, actionName);
        String pageForward = null;
        if (modelAction != null) {
            pageForward = modelAction.forwardToPage();
        }
        if (StringUtil.isBlank(pageForward)) {
            pageForward = "default";
        }

        if (isManageAction(request)) {
            String manageAppName = request.getId();
            if (App.SYS_MODEL_APP.equals(modelName)) {
                request.setManageType(ConsoleConstants.MANAGE_TYPE_APP);
            } else if (ConsoleConstants.MODEL_NAME_node.equals(modelName)) {
                request.setManageType(ConsoleConstants.MANAGE_TYPE_NODE);
                manageAppName = App.SYS_NODE_LOCAL;
            }
            request.setAppName(manageAppName);
            request.setModelName(App.SYS_MODEL_HOME);
            request.setActionName(Showable.ACTION_NAME_SHOW);
            if (App.SYS_NODE_LOCAL.equals(manageAppName)) {
                manageAppName = App.SYS_APP_NODE_AGENT;
            }
            ConsoleWarHelper.invokeLocalApp(manageAppName, request, response);// todo 对于远程的，这里数据不对?
        }

        String forwardToPage = HtmlView.htmlPageBase + (pageForward.contains("/") ? (pageForward + ".jsp") : ("view/" + pageForward + ".jsp"));
        restContext.servletRequest.getRequestDispatcher(forwardToPage).forward(restContext.servletRequest, restContext.servletResponse);
    }

    private boolean isManageAction(Request request) {
        if (!App.SYS_ACTION_MANAGE.equals(request.getActionName())) return false;

        if (!App.SYS_APP_MASTER.equals(request.getAppName())) return false;

        return App.SYS_MODEL_APP.equals(request.getModelName())
                || App.SYS_MODEL_NODE.equals(request.getModelName());
    }

    @Override
    public String getContentType() {
        return "text/html;charset=UTF-8";
    }
}
