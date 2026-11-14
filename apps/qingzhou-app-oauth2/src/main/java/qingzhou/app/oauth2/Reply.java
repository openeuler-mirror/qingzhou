package qingzhou.app.oauth2;

import java.util.LinkedHashMap;
import java.util.Map;

import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;

/** 端点响应：JSON 正文、错误码与重定向。 */
final class Reply {
    private Reply() {
    }

    static void send(HttpResponse response, Json json, Map<String, Object> body) throws Exception {
        response.header("Cache-Control", "no-store")
                .header("Pragma", "no-cache")
                .contentTypeJsonUtf8().send(json.toJson(body));
    }

    static void sendError(HttpResponse response, Json json, String code, String description) throws Exception {
        response.status(400);
        send(response, json, error(code, description));
    }

    static void sendUnauthorized(HttpResponse response, Json json, String code, String description) throws Exception {
        response.status(401)
                .header("WWW-Authenticate", "Bearer error=\"" + code + "\"");
        send(response, json, error(code, description));
    }

    static void sendClientUnauthorized(HttpResponse response, Json json) throws Exception {
        response.status(401)
                .header("WWW-Authenticate", "Basic realm=\"oauth2\"");
        send(response, json, error("invalid_client", "客户端验证失败"));
    }

    static void tooManyRequests(HttpResponse response, Json json) throws Exception {
        response.status(429).header("Retry-After", "300");
        send(response, json, error("temporarily_unavailable", "客户端认证失败次数过多，请稍后再试"));
    }

    static void methodNotAllowed(HttpResponse response, String allow) {
        response.status(405).header("Allow", allow).send("method not allowed");
    }

    static void redirect(HttpResponse response, String location) {
        response.status(302)
                .header("Location", location)
                .header("Cache-Control", "no-store")
                .header("Pragma", "no-cache")
                .send("redirecting");
    }

    static Map<String, Object> error(String code, String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", code);
        body.put("error_description", description);
        return body;
    }
}
