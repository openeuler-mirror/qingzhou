package qingzhou.console.view;

import qingzhou.api.MsgType;
import qingzhou.api.Response;
import qingzhou.console.controller.I18n;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.view.type.FileView;
import qingzhou.console.view.type.HtmlView;
import qingzhou.console.view.type.ImageView;
import qingzhou.console.view.type.JsonView;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.RequestImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ViewManager {
    public static final String htmlView = "html";
    public static final String fileView = "file";
    public static final String imageView = "image";
    private final Map<String, View> views = new HashMap<>();

    public ViewManager() {
        views.put(htmlView, new HtmlView());
        views.put(DeployerConstants.jsonView, new JsonView());
        views.put(fileView, new FileView());
        views.put(imageView, new ImageView());
    }

    public void render(RestContext restContext) throws Exception {
        RequestImpl request = restContext.request;
        Response response = restContext.request.getResponse();
        // 完善响应的 msg
        if (response.getMsg() == null) {
            String appName = SystemController.getAppName(request);
            String SP = I18n.isZH() ? "" : " ";
            String msg = response.isSuccess() ? I18n.getKeyI18n("msg.success") : I18n.getKeyI18n("msg.fail");
            String model = I18n.getModelI18n(appName, "model." + request.getModel());
            String action = I18n.getModelI18n(appName, "model.action." + request.getModel() + "." + request.getAction());
            String operation = Objects.equals(model, action) ? model : model + SP + action;
            response.setMsg(operation + SP + msg);
        }
        // 完善响应的 msg type
        if (response.getMsgType() == null) {
            response.setMsgType(response.isSuccess() ? MsgType.info : MsgType.error);
        }

        // 作出响应
        View view = views.get(request.getView());
        if (view == null) {
            throw new IllegalArgumentException("View not found: " + request.getView());
        }

        String contentType = response.getContentType();
        restContext.resp.setContentType((contentType == null || contentType.isEmpty()) ? view.getContentType() : contentType);
        view.render(restContext);
    }
}
