package qingzhou.app.oauth2;

import java.util.LinkedHashMap;
import java.util.Map;

import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;

/**
 * 端点响应：JSON 正文、错误码与重定向。
 */
final class Reply {
    private Reply() {
    }

    static void send(HttpResponse response, Json json, Map<String, Object> body) throws Exception {
        response.contentTypeJsonUtf8().sendFinish(json.toJson(body));
    }

    static void sendError(HttpResponse response, Json json, String code, String description) throws Exception {
        response.status(400);
        send(response, json, error(code, description));
    }

    static void sendUnauthorized(HttpResponse response, Json json, String code, String description) throws Exception {
        response.status(401);
        send(response, json, error(code, description));
    }

    static void redirect(HttpResponse response, String location) {
        response.status(302)
                .header("Location", location)
                .header("Cache-Control", "no-store")
                .sendFinish("redirecting");
    }

    static Map<String, Object> error(String code, String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", code);
        body.put("error_description", description);
        return body;
    }
}
