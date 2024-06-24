package qingzhou.console.view.type;

import java.io.ByteArrayOutputStream;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.view.View;

public class ImageView implements View {
    public static final String CONTENT_TYPE = "image/png";

    @Override
    public void render(RestContext restContext) throws Exception {
        String contentType = restContext.response.getContentType();
        if (contentType != null && !contentType.isEmpty()) {
            restContext.servletResponse.setContentType(contentType);
        } else {
            restContext.servletResponse.setContentType(CONTENT_TYPE);
        }
        restContext.servletResponse.getOutputStream().write(((ByteArrayOutputStream)restContext.response.getOutputStream()).toByteArray());
    }

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }
}