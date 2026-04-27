package qingzhou.http.impl;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpServer;

@Component
public class HttpServerImpl implements HttpServer {
    @Reference
    private HttpServiceEngine httpServiceEngine;

    @Override
    public void registerHttpHandler(HttpHandler httpHandler, String handlePath) {
        httpServiceEngine.addHttpHandler(httpHandler, new HashMap<String, String>() {{
            put(HttpHandler.HANDLE_PATH, handlePath);
        }});
    }

    @Override
    public void unregisterHttpHandler(HttpHandler httpHandler) {
        for (Map.Entry<String, HttpHandler> entry : httpServiceEngine.handlerMap.entrySet()) {
            if (entry.getValue().equals(httpHandler)) {
                String key = entry.getKey();
                httpServiceEngine.handlerMap.remove(key);
                break;
            }
        }
    }
}
