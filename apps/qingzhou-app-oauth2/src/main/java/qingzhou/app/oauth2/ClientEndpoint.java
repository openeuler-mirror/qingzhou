package qingzhou.app.oauth2;

import java.util.Map;

import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;

/**
 * 客户端端点模板：统一 POST 限定与客户端鉴权。
 */
abstract class ClientEndpoint implements HttpHandler {
    protected final Store store;
    protected final Json json;

    ClientEndpoint(Store store, Json json) {
        this.store = store;
        this.json = json;
    }

    @Override
    public final void handle(HttpRequest request, HttpResponse response) throws Exception {
        if (!"POST".equals(request.getMethod())) { // 凭据不得经 GET 进入 URL 与访问日志
            Reply.methodNotAllowed(response);
            return;
        }

        Map<String, String> client = store.authenticateClient(
                request.getParameter("client_id"), request.getParameter("client_secret"));
        if (client == null) {
            Reply.sendError(response, json, "invalid_client", "客户端验证失败");
            return;
        }
        process(request, client, response);
    }

    protected abstract void process(HttpRequest request, Map<String, String> client, HttpResponse response) throws Exception;
}
