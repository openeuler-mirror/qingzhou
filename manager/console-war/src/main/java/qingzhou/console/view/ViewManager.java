package qingzhou.console.view;

import qingzhou.console.RequestImpl;
import qingzhou.console.ResponseImpl;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.i18n.ConsoleI18n;
import qingzhou.console.i18n.I18n;
import qingzhou.console.page.PageBackendService;
import qingzhou.console.view.type.FileView;
import qingzhou.console.view.type.HtmlView;
import qingzhou.console.view.type.JsonView;
import qingzhou.console.view.type.ImageView;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ViewManager {
    public static final String htmlView = "html";
    public static final String jsonView = "json";
    public static final String fileView = "file";
    public static final String imageView = "image";
    private final Map<String, View> views = new HashMap<>();

    public ViewManager() {
        views.put(htmlView, new HtmlView());
        views.put(jsonView, new JsonView());
        views.put(fileView, new FileView());
        views.put(imageView, new ImageView());
    }

    public void render(RestContext restContext) throws Exception {
        RequestImpl request = restContext.request;
        ResponseImpl response = restContext.response;
        // 完善响应的 msg
        if (response.getMsg() == null) {
            String appName = PageBackendService.getAppName(request);
            String SP = I18n.isZH() ? "" : " ";
            String msg = response.isSuccess() ? ConsoleI18n.getI18n(I18n.getI18nLang(), "msg.success") : ConsoleI18n.getI18n(I18n.getI18nLang(), "msg.fail");
            String model = I18n.getString(appName, "model." + request.getModel());
            String action = I18n.getString(appName, "model.action." + request.getModel() + "." + request.getAction());
            String operation = Objects.equals(model, action) ? model : model + SP + action;
            response.setMsg(operation + SP + msg);
        }

        // 作出响应
        View view = views.get(request.getView());
        if (view == null) {
            throw new IllegalArgumentException("View not found: " + request.getView());
        }

        String contentType = response.getContentType();
        restContext.servletResponse.setContentType((contentType == null || contentType.isEmpty()) ? view.getContentType() : contentType);
        view.render(restContext);
    }
}
