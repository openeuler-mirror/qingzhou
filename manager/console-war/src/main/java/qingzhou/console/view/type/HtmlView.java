package qingzhou.console.view.type;

import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.api.type.*;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.view.View;
import qingzhou.deployer.ActionInvoker;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.RequestImpl;
import qingzhou.deployer.ResponseImpl;
import qingzhou.engine.util.Utils;
import qingzhou.registry.ModelActionInfo;
import qingzhou.registry.ModelFieldInfo;
import qingzhou.registry.ModelInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public class HtmlView implements View {
    public static final String htmlPageBase = "/WEB-INF/view/";
    public static final String FLAG = "html";

    @Override
    public void render(RestContext restContext) throws Exception {
        HttpServletRequest servletRequest = restContext.req;
        HttpServletResponse servletResponse = restContext.resp;
        if (servletResponse.isCommitted()) return;

        RequestImpl request = restContext.request;
        ModelInfo modelInfo = request.getCachedModelInfo();
        ModelActionInfo actionInfo = modelInfo.getModelActionInfo(request.getAction());

        String page = actionInfo.getPage();
        if (Utils.notBlank(page)) {
            page = "/" + request.getApp() + (page.startsWith("/") ? page : "/" + page);
            servletRequest.getRequestDispatcher(page).forward(servletRequest, restContext.resp);
            return;
        }

        String redirect = actionInfo.getRedirect();
        if (Utils.notBlank(redirect)) {
            servletResponse.sendRedirect(RESTController.encodeURL(servletResponse, servletRequest.getContextPath() +
                    DeployerConstants.REST_PREFIX +
                    "/" + request.getView() +
                    "/" + request.getApp() +
                    "/" + request.getModel() +
                    "/" + redirect +
                    "/" + request.getId()));
            return;
        }

        // 转发给内部的 jsp view
        servletRequest.setAttribute(Request.class.getName(), request);
        boolean isManageAction = isManageApp(request);
        if (isManageAction) {
            request.setAppName(request.getId());
            request.setModelName("home"); // qingzhou.app.common.Home 的 code
            request.setCachedModelInfo(SystemController.getModelInfo(request.getApp(), request.getModel())); // 重新缓存
            request.setActionName(Show.ACTION_SHOW);
            Response response = SystemController.getService(ActionInvoker.class).invokeSingle(request);
            request.setResponse(response);// 用远端的请求替换本地的，如果是本地实例，它俩是等效的
        }

        doMonitorData(request);

        String forwardView = isManageAction ? "sys/manage" : getForwardView(request);
        String forwardToPage = HtmlView.htmlPageBase + (forwardView.contains("/") ? (forwardView + ".jsp") : ("type/" + forwardView + ".jsp"));
        restContext.req.getRequestDispatcher(forwardToPage).forward(restContext.req, restContext.resp);
    }

    private void doMonitorData(RequestImpl request) {
        if (!request.getAction().equals(Monitor.ACTION_MONITOR)) return;
        ResponseImpl response = (ResponseImpl) request.getResponse();

        Map<String, String> monitorData = new HashMap<>();
        Map<String, String> infoData = new HashMap<>();

        ModelInfo modelInfo = request.getCachedModelInfo();
        String[] monitorFieldNames = modelInfo.getMonitorFieldNames();
        for (String fieldName : monitorFieldNames) {
            String val = response.getDataMap().get(fieldName);
            if (val == null) continue;
            ModelFieldInfo monitorField = modelInfo.getModelFieldInfo(fieldName);
            if (monitorField.isNumeric()) {
                monitorData.put(fieldName, val);
            } else {
                infoData.put(fieldName, val);
            }
        }
        response.getDataMap().clear(); // 监视页面是用的 DataList，这里清空不需要的 DataMap，注意 json view 里面是没有区分的
        response.addDataList(monitorData);
        response.addDataList(infoData);
    }

    private boolean isManageApp(Request request) {
        if (!DeployerConstants.ACTION_MANAGE.equals(request.getAction())) return false;
        if (!DeployerConstants.APP_SYSTEM.equals(request.getApp())) return false;
        return DeployerConstants.MODEL_APP.equals(request.getModel());
    }

    @Override
    public String getContentType() {
        return "text/html;charset=UTF-8";
    }

    private String getForwardView(RequestImpl request) {
        String actionName = request.getAction();
        switch (actionName) {
            case Show.ACTION_SHOW:
                return "show";
            case Add.ACTION_CREATE:
            case Update.ACTION_EDIT:
                return "form";
            case List.ACTION_LIST:
                return "list";
            case Monitor.ACTION_MONITOR:
                return "monitor";
            case DeployerConstants.ACTION_INDEX:
                return "sys/index";
            case DeployerConstants.ACTION_MANAGE:
                return "sys/manage";
        }

        return "default";
    }
}
