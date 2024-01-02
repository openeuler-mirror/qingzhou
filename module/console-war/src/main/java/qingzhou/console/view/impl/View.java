package qingzhou.console.view.impl;

import qingzhou.console.controller.rest.RestContext;

public interface View {
    void render(RestContext restContext) throws Exception;

    String getContentType();
}
