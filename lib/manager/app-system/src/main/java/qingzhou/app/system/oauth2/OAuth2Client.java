package qingzhou.app.system.oauth2;

import qingzhou.config.OAuth2;
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
    private final HttpClient httpClient = SystemController.getService(Http.class).buildHttpClient();
    private final Json jsonService = SystemController.getService(Json.class);

    static OAuth2Client getInstance(OAuth2 config) {
        if (!config.isEnabled()) return null;

        AuthPolicy authPolicy = new DefaultAuthPolicy();
        return new OAuth2Client(config, authPolicy);
    }

    private final OAuth2 config;
    private final AuthPolicy serverVendor;

    private OAuth2Client(OAuth2 config, AuthPolicy serverVendor) {
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
                .param("client_id", config.getClient_id())
                .param("redirect_uri", config.getReceiveCodeUrl());

        String baseUrl = config.getAuthorizeUrl();
        return baseUrl + (baseUrl.contains("?") ? "&" : "?") + toUrl(builder.params());
    }

    public String login(String code) throws Exception {
        if (code == null) return null;
        RequestBuilder builder = RequestBuilder.builder()
                .param("client_id", config.getClient_id())
                .param("client_secret", config.getClientSecret())
                .param("grant_type", "authorization_code")
                .param("code", code);

        return serverVendor.accessToken(config, builder).getAccessToken();
    }

    public String getUserInfo(String accessToken) throws Exception {
        RequestBuilder builder = RequestBuilder.builder().param("client_id", config.getClient_id());
        return serverVendor.userInfo(config, builder, accessToken).getUsername();
    }

    public boolean checkToken(String token) throws Exception {
        if (Utils.notBlank(config.getCheckTokenUrl())) {
            return serverVendor.checkToken(config, token);
        } else {
            return true;
        }
    }

    public String toUrl(Map<String, String> params) {
        StringBuilder url = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            url.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        return url.substring(0, url.length() - 1);
    }

    private interface AuthPolicy {
        Response logout(OAuth2 config, RequestBuilder builder) throws Exception;

        Response accessToken(OAuth2 config, RequestBuilder builder) throws Exception;

        Response userInfo(OAuth2 config, RequestBuilder builder, String accessToken) throws Exception;

        default boolean checkToken(OAuth2 config, String accessToken) throws Exception {
            return true;
        }
    }

    private class DefaultAuthPolicy implements AuthPolicy {

        @Override
        public Response logout(AuthPolicy config, RequestBuilder builder) throws Exception {
            builder.responseType(TongResponse.class);
            return sendReq(config.getLogoutUrl(), builder);
        }

        @Override
        public Response accessToken(AuthPolicy config, RequestBuilder builder) throws Exception {
            builder.responseType(TongResponse.class);
            return sendReq(config.getTokenUrl(), builder);
        }

        @Override
        public Response userInfo(AuthPolicy config, RequestBuilder builder, String accessToken) throws Exception {
            builder.method("GET")
                    .param("listen_logout", config.getListenLogout())
                    .header("Authorization", "Bearer " + accessToken)
                    .responseType(TongResponse.class);
            return sendReq(config.getUserInfoUrl(), builder);
        }

        @Override
        public boolean checkToken(AuthPolicy config, String accessToken) throws Exception {
            String checkTokenUrl = config.getCheckTokenUrl();
            RequestBuilder builder = RequestBuilder.builder()
                    .param("client_id", config.getClient_id())
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

    public class TongResponse extends HashMap<String, Object> implements Response {

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
