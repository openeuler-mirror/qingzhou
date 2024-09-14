package qingzhou.console.view;

import qingzhou.api.Response;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.view.type.FileView;
import qingzhou.console.view.type.HtmlView;
import qingzhou.console.view.type.ImageView;
import qingzhou.console.view.type.JsonView;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.RequestImpl;

import java.util.HashMap;
import java.util.Map;

public class ViewManager {
    public static final String htmlView = "html";
    public static final String fileView = "file";
    public static final String imageView = "image";
    private final Map<String, View> views = new HashMap<>();

    public ViewManager() {
        views.put(htmlView, new HtmlView());
        views.put(DeployerConstants.JSON_VIEW, new JsonView());
        views.put(fileView, new FileView());
        views.put(imageView, new ImageView());
    }

    public void render(RestContext restContext) throws Exception {
        RequestImpl request = restContext.request;
        Response response = restContext.request.getResponse();

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
