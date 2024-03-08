package qingzhou.console.view;

import qingzhou.console.controller.rest.RestContext;

public interface View {
    void render(RestContext restContext) throws Exception;

    String getContentType();
}
