package qingzhou.app.oauth2;

import java.util.Map;

import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;

/**
 * 客户端端点模板：统一 POST 限定、Basic/POST 客户端认证与凭据错误响应。
 */
abstract class ClientEndpoint implements HttpHandler {
    private static final Throttle CLIENT_THROTTLE = new Throttle();

    protected final Store store;
    protected final Json json;
    private final boolean allowAnonymous;

    ClientEndpoint(Store store, Json json) {
        this(store, json, false);
    }

    ClientEndpoint(Store store, Json json, boolean allowAnonymous) {
        this.store = store;
        this.json = json;
        this.allowAnonymous = allowAnonymous;
    }

    @Override
    public final void handle(HttpRequest request, HttpResponse response) throws Exception {
        if (!"POST".equals(request.getMethod())) { // 凭据不得经 GET 进入 URL 与访问日志
            Reply.methodNotAllowed(response, "POST");
            return;
        }

        Security.ClientAuth auth = Security.clientAuth(request);
        String throttleKey = "client|" + request.getRemoteHost() + '|'
                + (auth.clientId == null ? "" : auth.clientId);
        if (CLIENT_THROTTLE.isLocked(throttleKey)) {
            Reply.tooManyRequests(response, json);
            return;
        }

        Map<String, String> client = auth.malformed || Security.isEmpty(auth.clientId)
                ? null : store.findClient(auth.clientId);
        if (!Security.clientAuthAllowed(store, client, auth)) {
            // 仅 Revoke 可开启：完全未携带客户端凭据时，按 token 持有者注销；
            // 携带了 client_id/secret 但校验失败仍按 401 处理，不能借匿名模式绕过错误凭据。
            if (allowAnonymous && !auth.malformed && "none".equals(auth.method)
                    && Security.isEmpty(auth.clientId) && Security.isEmpty(auth.secret)) {
                process(request, null, response);
                return;
            }
            CLIENT_THROTTLE.recordFailure(throttleKey);
            Reply.sendClientUnauthorized(response, json);
            return;
        }
        CLIENT_THROTTLE.clear(throttleKey);
        process(request, client, response);
    }

    protected abstract void process(HttpRequest request, Map<String, String> client, HttpResponse response) throws Exception;
}
