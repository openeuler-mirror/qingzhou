package qingzhou.app.oauth2;

import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;

/**
 * OIDC JWKS：发布 ID Token 验签公钥。
 */
public class Jwks implements HttpHandler {
    private final Oidc oidc;
    private final Json json;

    public Jwks(Oidc oidc, Json json) {
        this.oidc = oidc;
        this.json = json;
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response) throws Exception {
        if (!"GET".equals(request.getMethod())) {
            Reply.methodNotAllowed(response, "GET");
            return;
        }
        Reply.send(response, json, oidc.jwks());
    }
}
