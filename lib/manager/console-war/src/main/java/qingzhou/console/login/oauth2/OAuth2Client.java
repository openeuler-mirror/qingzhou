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
    private static final Json jsonService = SystemController.getService(Json.class);

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

    public boolean logout(String accessToken) throws Exception {
        RequestBuilder requestBuilder = RequestBuilder.builder()
                .accessToken(accessToken);

        serverVendor.logout(config, requestBuilder);
        return sendReq(config.getLogoutUrl(), requestBuilder).success();
    }

    public String getLoginUrl() {
        RequestBuilder builder = RequestBuilder.builder()
                .param("response_type", "code")
                .clientId(config.getClientId())
                .redirectUrl(config.getReceiveCodeUrl());

        String baseUrl = config.getAuthorizeUrl();
        return baseUrl + (baseUrl.contains("?") ? "&" : "?") + toUrl(builder.params());
    }

    public String[] login(String code) throws Exception {
        if (code == null) return null;
        RequestBuilder builder = RequestBuilder.builder()
                .clientId(config.getClientId())
                .clientSecret(config.getClientSecret())
                .grantType("authorization_code")
                .code(code);

        serverVendor.accessToken(config, builder);
        Response response = sendReq(config.getTokenUrl(), builder);
        String accessToken = response.getAccessToken();

        String userId = getUserInfo(accessToken);
        String expiresIn = String.valueOf(response.getExpiresIn());
        return new String[]{accessToken, userId, expiresIn};
    }

    public String getUserInfo(String accessToken) throws Exception {
        RequestBuilder builder = RequestBuilder.builder()
                .clientId(config.getClientId());
        serverVendor.userInfo(config, builder, accessToken);
        Response response = sendReq(config.getUserInfoUrl(), builder);
        return response.getUsername();
    }

    public boolean checkToken(String token) throws Exception {
        RequestBuilder builder = RequestBuilder.builder()
                .clientId(config.getClientId())
                .clientSecret(config.getClientSecret());

        serverVendor.checkToken(config, builder, token);

        String checkTokenUrl = config.getCheckTokenUrl();
        if (Utils.notBlank(checkTokenUrl)) {
            Response response = sendReq(checkTokenUrl, builder);
            return response.active();
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
        void logout(OAuthConfig config, RequestBuilder builder);

        void accessToken(OAuthConfig config, RequestBuilder builder);

        void userInfo(OAuthConfig config, RequestBuilder builder, String accessToken);

        default void checkToken(OAuthConfig config, RequestBuilder builder, String accessToken) {
        }
    }

    private static class TongAuth implements ServerPolicy {

        @Override
        public void logout(OAuthConfig config, RequestBuilder builder) {
            builder.responseType(TongResponse.class);
        }

        @Override
        public void accessToken(OAuthConfig config, RequestBuilder builder) {
            builder.responseType(TongResponse.class);
        }

        @Override
        public void userInfo(OAuthConfig config, RequestBuilder builder, String accessToken) {
            builder.method("GET")
                    .param("listen_logout", config.getListenLogout())
                    .header("Authorization", "Bearer " + accessToken)
                    .responseType(TongResponse.class);
        }

        @Override
        public void checkToken(OAuthConfig config, RequestBuilder builder, String accessToken) {
            builder.param("token", accessToken)
                    .responseType(TongResponse.class);
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

        public RequestBuilder clientId(String clientId) {
            params.put("client_id", clientId);
            return this;
        }

        public RequestBuilder clientSecret(String clientSecret) {
            params.put("client_secret", clientSecret);
            return this;
        }

        public RequestBuilder grantType(String grantType) {
            params.put("grant_type", grantType);
            return this;
        }

        public RequestBuilder accessToken(String accessToken) {
            params.put("access_token", accessToken);
            return this;
        }

        public RequestBuilder code(String code) {
            params.put("code", code);
            return this;
        }

        public RequestBuilder redirectUrl(String redirectUrl) {
            params.put("redirect_uri", redirectUrl);
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

        default boolean active() {
            return true;
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
        public boolean active() {
            return Boolean.parseBoolean(String.valueOf(get("active")));
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
