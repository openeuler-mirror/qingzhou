package qingzhou.console.view;

import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.i18n.ConsoleI18n;
import qingzhou.console.i18n.I18n;
import qingzhou.console.page.PageBackendService;
import qingzhou.console.view.type.FileView;
import qingzhou.console.view.type.HtmlView;
import qingzhou.console.view.type.JsonView;
import qingzhou.console.RequestImpl;
import qingzhou.console.ResponseImpl;
import qingzhou.engine.util.StringUtil;

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

    public void render(RestContext restContext) throws Exception {
        RequestImpl request = restContext.request;
        ResponseImpl response = restContext.response;
        // 完善响应的 msg
        if (StringUtil.isBlank(response.getMsg())) {
            String appName = PageBackendService.getAppName(request);
            String SP = I18n.isZH() ? "" : " ";
            String msg = response.isSuccess() ? ConsoleI18n.getI18n(I18n.getI18nLang(), "msg.success") : I18n.getString(appName, "msg.fail");
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

        restContext.servletResponse.setContentType(view.getContentType());
        view.render(restContext);

    }
}
