package qingzhou.console.view;

import qingzhou.console.RequestImpl;
import qingzhou.console.ResponseImpl;
import qingzhou.console.controller.RestContext;
import qingzhou.console.util.Constants;
import qingzhou.console.util.StringUtil;
import qingzhou.console.view.impl.FileView;
import qingzhou.console.view.impl.HtmlView;
import qingzhou.console.view.impl.JsonView;
import qingzhou.console.view.impl.View;
import qingzhou.framework.app.I18n;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ViewManager {
    public static final String htmlView = "html";
    public static final String jsonView = "json";
    public static final String fileView = "file";
    private final Map<String, View> views = new HashMap<>();

    public ViewManager() {
        views.put(htmlView, new HtmlView());
        views.put(jsonView, new JsonView());
        views.put(fileView, new FileView());
    }

    public boolean render(RestContext restContext) throws Exception {
        RequestImpl request = (RequestImpl) restContext.request;
        ResponseImpl response = (ResponseImpl) restContext.response;
        // 完善响应的 msg
        if (StringUtil.isBlank(response.getMsg())) {
            String appName = request.getAppName();
            String SP = I18n.getI18nLang().isZH() ? "" : " ";
            String msg = response.isSuccess() ? I18n.getString(Constants.QINGZHOU_MASTER_APP_NAME, "msg.success") : I18n.getString(appName, "msg.fail");
            String model = I18n.getString(appName, "model." + request.getModelName());
            String action = I18n.getString(appName, "model.action." + request.getModelName() + "." + request.getActionName());
            String operation = Objects.equals(model, action) ? model : model + SP + action;
            response.setMsg(operation + SP + msg);
        }

        // 作出响应
        View view = views.get(request.getViewName());
        if (view == null) {
            throw new IllegalArgumentException("View not found: " + request.getViewName());
        }

        if (StringUtil.isBlank(response.getContentType())) {
            response.setContentType(view.getContentType());
            restContext.servletResponse.setContentType(view.getContentType());
        }
        view.render(restContext);

        return true;
    }
}
