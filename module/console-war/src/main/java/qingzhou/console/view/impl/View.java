package qingzhou.console.view.impl;

import qingzhou.console.controller.RestContext;

public interface View {
    void render(RestContext restContext) throws Exception;

    String getContentType();
}
