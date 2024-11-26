package qingzhou.console.login;

import qingzhou.console.controller.SystemController;
import qingzhou.engine.util.Utils;
import qingzhou.http.Http;
import qingzhou.http.HttpClient;
import qingzhou.http.HttpResponse;
import qingzhou.json.Json;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class OAuth2Client {
    private static final HttpClient httpClient = SystemController.getService(Http.class).buildHttpClient();
    private static final Json jsonService = SystemController.getService(Json.class);

    public static OAuth2Client getInstance(OAuthConfig config) {
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

    public OAuthConfig getConfig() {
        return config;
    }

    private Response sendReq(String url, RequestBuilder requestBuilder) throws Exception {
        String method = requestBuilder.method();
        requestBuilder.header("Accept", "application/json");
        HttpResponse res;
        if ("GET".equals(method)) {
            StringBuilder queryStr = new StringBuilder();
            for (Map.Entry<String, String> entry : requestBuilder.params().entrySet()) {
                queryStr.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }

            res = httpClient.get(url + "?" + queryStr, requestBuilder.headers());
        } else {
            res = httpClient.post(url, requestBuilder.params(), requestBuilder.headers());
        }

        return jsonService.fromJson(new String(res.getResponseBody(), StandardCharsets.UTF_8), requestBuilder.responseType());
    }

    public boolean logout(String accessToken) throws Exception {
        RequestBuilder requestBuilder = RequestBuilder.builder()
                .accessToken(accessToken);

        serverVendor.logout(config, requestBuilder);
        Response response = sendReq(config.getLogoutUrl(), requestBuilder);
        return response.success();
    }

    public String getLoginUrl() {
        RequestBuilder builder = RequestBuilder.builder()
                .param("response_type", "code")
                .clientId(config.getClientId())
                .redirectUrl(config.getReceiveCodeUrl());

        serverVendor.buildLoginUrl(config, builder);
        String baseUrl = config.getAuthorizeUrl();
        String first = baseUrl.contains("?") ? "&" : "?";
        StringBuilder url = new StringBuilder(baseUrl).append(first);
        for (Map.Entry<String, String> entry : builder.params().entrySet()) {
            url.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }

        return url.toString();
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

    private interface ServerPolicy {
        void logout(OAuthConfig config, RequestBuilder builder);

        void accessToken(OAuthConfig config, RequestBuilder builder);

        void userInfo(OAuthConfig config, RequestBuilder builder, String accessToken);

        default void checkToken(OAuthConfig config, RequestBuilder builder, String accessToken) {
        }

        default void buildLoginUrl(OAuthConfig config, RequestBuilder builder) {
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


    public static class OAuthConfig {
        private boolean enabled;
        private String redirectUrl;     // 当前应用域名
        private String clientId;
        private String clientSecret;
        private String authorizeUrl;    // 授权地址
        private String tokenUrl;        // 获取token地址
        private String checkTokenUrl;   // 检验token地址
        private String userInfoUrl;         // 获取用户信息地址
        private String logoutUrl;       // 注销地址
        private String listenLogout;    // 注销回调地址
        private String receiveCodeUrl;
        private final Properties properties;  // 配置属性，非公用的属性
        private String serverVendor;

        public OAuthConfig(Properties properties) {
            this.enabled = Boolean.parseBoolean(properties.getProperty("enabled", "false"));
            this.redirectUrl = properties.getProperty("redirectUrl");
            this.clientId = properties.getProperty("clientId");
            this.clientSecret = properties.getProperty("clientSecret");
            this.authorizeUrl = properties.getProperty("authorizeUrl");
            this.tokenUrl = properties.getProperty("tokenUrl");
            this.checkTokenUrl = properties.getProperty("checkTokenUrl");
            this.userInfoUrl = properties.getProperty("userInfoUrl");
            this.logoutUrl = properties.getProperty("logoutUrl");
            this.serverVendor = properties.getProperty("serverVendor");
            this.properties = properties;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getAuthorizeUrl() {
            return authorizeUrl;
        }

        public void setAuthorizeUrl(String authorizeUrl) {
            this.authorizeUrl = authorizeUrl;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getCheckTokenUrl() {
            return checkTokenUrl;
        }

        public void setCheckTokenUrl(String checkTokenUrl) {
            this.checkTokenUrl = checkTokenUrl;
        }

        public String getLogoutUrl() {
            return logoutUrl;
        }

        public void setLogoutUrl(String logoutUrl) {
            this.logoutUrl = logoutUrl;
        }

        public String getTokenUrl() {
            return tokenUrl;
        }

        public void setTokenUrl(String tokenUrl) {
            this.tokenUrl = tokenUrl;
        }

        public String getRedirectUrl() {
            return redirectUrl;
        }

        public void setRedirectUrl(String redirectUrl) {
            this.redirectUrl = redirectUrl;
        }

        public String getUserInfoUrl() {
            return userInfoUrl;
        }

        public void setUserInfoUrl(String userInfoUrl) {
            this.userInfoUrl = userInfoUrl;
        }

        public String getServerVendor() {
            return serverVendor;
        }

        public void setServerVendor(String serverVendor) {
            this.serverVendor = serverVendor;
        }

        public String getListenLogout() {
            return listenLogout;
        }

        public void setListenLogout(String listenLogout) {
            this.listenLogout = listenLogout;
        }

        public String getReceiveCodeUrl() {
            return receiveCodeUrl;
        }

        public void setReceiveCodeUrl(String receiveCodeUrl) {
            this.receiveCodeUrl = receiveCodeUrl;
        }

        public String getConfig(String key) {
            return properties.getProperty(key);
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
