package qingzhou.console.view;

import qingzhou.console.RestContext;

public interface View {
    void render(RestContext restContext) throws Exception;

    String getContentType();
}
