package qingzhou.framework.app;

import qingzhou.framework.api.AppContext;
import qingzhou.framework.api.QingZhouApp;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;

import java.util.Properties;

public interface App {
    QingZhouApp getQingZhouApp();

    AppContext getAppContext();

    Properties getAppProperties();

    void invoke(Request request, Response response) throws Exception;
}