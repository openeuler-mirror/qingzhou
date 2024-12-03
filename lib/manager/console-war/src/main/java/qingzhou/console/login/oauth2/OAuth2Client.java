package qingzhou.console.login.oauth2;

import qingzhou.console.controller.SystemController;
import qingzhou.engine.util.Utils;
import qingzhou.http.Http;
import qingzhou.http.HttpClient;
import qingzhou.http.HttpResponse;
import qingzhou.json.Json;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class OAuth2Client {

    private static final HttpClient httpClient = SystemController.getService(Http.class).buildHttpClient();
    public static final Json jsonService = SystemController.getService(Json.class);

    static OAuth2Client getInstance(OAuthConfig config) {
        if (!config.isEnabled()) return null;

        ServerPolicy serverPolicy;
        if (Utils.isBlank(config.getServerVendor())) {
            serverPolicy = new TongAuth();
        } else {
            return null;
        }

        return new OAuth2Client(config, serverPolicy);
    }

    private final OAuthConfig config;
    private final ServerPolicy serverVendor;

    private OAuth2Client(OAuthConfig config, ServerPolicy serverVendor) {
        this.config = config;
        this.serverVendor = serverVendor;
    }

    public boolean logout(String accessToken) throws Exception {
        RequestBuilder requestBuilder = RequestBuilder.builder().param("access_token", accessToken);
        if (config.getLogoutUrl() != null) {
            return serverVendor.logout(config, requestBuilder).success();
        } else {
            return true;
        }
    }

    public String getLoginUrl() {
        RequestBuilder builder = RequestBuilder.builder()
                .param("response_type", "code")
                .param("client_id", config.getClientId())
                .param("redirect_uri", config.getReceiveCodeUrl());

        String baseUrl = config.getAuthorizeUrl();
        return baseUrl + (baseUrl.contains("?") ? "&" : "?") + toUrl(builder.params());
    }

    public String login(String code) throws Exception {
        if (code == null) return null;
        RequestBuilder builder = RequestBuilder.builder()
                .param("client_id", config.getClientId())
                .param("client_secret", config.getClientSecret())
                .param("grant_type", "authorization_code")
                .param("code", code);

        return serverVendor.accessToken(config, builder).getAccessToken();
    }

    public String getUserInfo(String accessToken) throws Exception {
        RequestBuilder builder = RequestBuilder.builder().param("client_id", config.getClientId());
        return serverVendor.userInfo(config, builder, accessToken).getUsername();
    }

    public boolean checkToken(String token) throws Exception {
        if (Utils.notBlank(config.getCheckTokenUrl())) {
            return serverVendor.checkToken(config, token);
        } else {
            return true;
        }
    }

    public static String toUrl(Map<String, String> params) {
        StringBuilder url = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            url.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        return url.substring(0, url.length() - 1);
    }

    private interface ServerPolicy {
        Response logout(OAuthConfig config, RequestBuilder builder) throws Exception;

        Response accessToken(OAuthConfig config, RequestBuilder builder) throws Exception;

        Response userInfo(OAuthConfig config, RequestBuilder builder, String accessToken) throws Exception;

        default boolean checkToken(OAuthConfig config, String accessToken) throws Exception {
            return true;
        }
    }

    private static class TongAuth implements ServerPolicy {

        @Override
        public Response logout(OAuthConfig config, RequestBuilder builder) throws Exception {
            builder.responseType(TongResponse.class);
            return sendReq(config.getLogoutUrl(), builder);
        }

        @Override
        public Response accessToken(OAuthConfig config, RequestBuilder builder) throws Exception {
            builder.responseType(TongResponse.class);
            return sendReq(config.getTokenUrl(), builder);
        }

        @Override
        public Response userInfo(OAuthConfig config, RequestBuilder builder, String accessToken) throws Exception {
            builder.method("GET")
                    .param("listen_logout", config.getListenLogout())
                    .header("Authorization", "Bearer " + accessToken)
                    .responseType(TongResponse.class);
            return sendReq(config.getUserInfoUrl(), builder);
        }

        @Override
        public boolean checkToken(OAuthConfig config, String accessToken) throws Exception {
            String checkTokenUrl = config.getCheckTokenUrl();
            RequestBuilder builder = RequestBuilder.builder()
                    .param("client_id", config.getClientId())
                    .param("client_secret", config.getClientSecret())
                    .param("token", accessToken)
                    .responseType(TongResponse.class);
            TongResponse response = (TongResponse) sendReq(checkTokenUrl, builder);
            return Boolean.parseBoolean(String.valueOf(response.get("active")));
        }

        private Response sendReq(String url, RequestBuilder requestBuilder) throws Exception {
            String method = requestBuilder.method();
            requestBuilder.header("Accept", "application/json");
            HttpResponse res;
            if ("GET".equals(method)) {
                res = httpClient.get(url + "?" + toUrl(requestBuilder.params()), requestBuilder.headers());
            } else {
                res = httpClient.post(url, requestBuilder.params(), requestBuilder.headers());
            }

            return jsonService.fromJson(new String(res.getResponseBody(), StandardCharsets.UTF_8), requestBuilder.responseType());
        }
    }

    public static class RequestBuilder {
        private final Map<String, String> params = new HashMap<>();
        private final Map<String, String> headers = new HashMap<>();
        private Class<? extends Response> clazz;
        private String method = "POST";

        public static RequestBuilder builder() {
            return new RequestBuilder();
        }

        public RequestBuilder method(String method) {
            this.method = method;
            return this;
        }

        public RequestBuilder param(String key, String value) {
            params.put(key, value);
            return this;
        }

        public RequestBuilder header(String key, String value) {
            headers.put(key, value);
            return this;
        }

        public RequestBuilder responseType(Class<? extends Response> clazz) {
            this.clazz = clazz;
            return this;
        }

        public String method() {
            return method;
        }

        public Map<String, String> params() {
            return params;
        }

        public Map<String, String> headers() {
            return headers;
        }

        public Class<? extends Response> responseType() {
            return clazz;
        }
    }

    public interface Response {
        default String getUsername() {
            return null;
        }

        default String getAccessToken() {
            return null;
        }

        default long getExpiresIn() {
            return 0;
        }

        default boolean success() {
            return false;
        }
    }

    public static class TongResponse extends HashMap<String, Object> implements Response {

        @Override
        public String getAccessToken() {
            return (String) get("access_token");
        }

        @Override
        public long getExpiresIn() {
            return ((Double) get("expires_in")).longValue();
        }

        @Override
        public boolean success() {
            return Boolean.parseBoolean(String.valueOf(get("success")));
        }

        @Override
        public String getUsername() {
            if (success()) {
                Map<String, Object> data = (Map<String, Object>) get("data");
                if (data != null) {
                    return (String) data.get("userName");
                }
            }
            return null;
        }
    }
}
