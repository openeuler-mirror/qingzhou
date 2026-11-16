package qingzhou.app.oauth;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import qingzhou.api.InputType;
import qingzhou.api.ModelBase;
import qingzhou.api.Request;
import qingzhou.api.annotations.Model;
import qingzhou.api.annotations.ModelAction;
import qingzhou.api.annotations.ModelField;
import qingzhou.api.action.Delete;
import qingzhou.api.action.Page;
import qingzhou.api.action.Show;
import qingzhou.logger.Logger;

@Model(code = "client", order = 1,
        name = {"接入客户端", "en:Client"},
        info = {"OAuth 2.0 / OIDC 接入方凭据、回调地址与授权策略",
                "en:OAuth 2.0 / OIDC client credentials, redirect uris and policy"},
        icon = "Grid",
        menu = "access")
public class ClientModel extends ModelBase implements Page, Show, Delete {
    private static final String GRANTS = "authorization_code,password,client_credentials,refresh_token,implicit";
    private static final String AUTH_METHODS = "client_secret_basic,client_secret_post,none";
    private static final String[] FORM_FIELDS = {
            "client_id", "client_secret", "client_name", "redirect_uri", "grant_types", "scope",
            "token_endpoint_auth_method", "require_pkce", "access_token_ttl", "refresh_token_ttl", "enabled"
    };

    @ModelField(id = true, list = true, readonly = true,
            name = {"编号", "en:ID"},
            info = {"客户端记录编号", "en:Client record id"})
    public String id;

    @ModelField(list = true, search = true, required = true,
            name = {"客户端 ID", "en:Client ID"},
            info = {"OAuth 客户端唯一标识，仅用于协议接入，不是授权页登录用户名；例如 test_client",
                    "en:OAuth client identifier for protocol access, not a consent-page login name"})
    public String client_id;

    @ModelField(input_type = InputType.password,
            name = {"客户端密钥", "en:Client Secret"},
            info = {"仅用于 client_secret_basic/post 客户端认证，不是用户密码；编辑时留空保留原值；none 可留空",
                    "en:Used only for client authentication, not a user password; blank on edit keeps the old value"})
    public String client_secret;

    @ModelField(list = true, required = true,
            name = {"客户端名称", "en:Client Name"},
            info = {"展示在授权确认页上的应用名称", "en:Name shown on the consent page"})
    public String client_name;

    @ModelField(list = true, input_type = InputType.textarea,
            name = {"回调地址", "en:Redirect URIs"},
            info = {"授权码/隐式模式必填，其他授权模式可留空；每个地址一行或逗号分隔，授权请求的 redirect_uri 必须与其中之一完全一致",
                    "en:Required for authorization_code/implicit; one URI per line or comma separated"})
    public String redirect_uri;

    @ModelField(list = true, required = true,
            input_type = InputType.multiselect, separator = ",",
            options = {"authorization_code", "password", "client_credentials", "refresh_token", "implicit"},
            name = {"授予类型", "en:Grant Types"},
            info = {"可多选：authorization_code（授权码模式，推荐，支持 PKCE）、password（密码模式）、"
                    + "client_credentials（客户端凭证，无需用户）、refresh_token（刷新令牌）、"
                    + "implicit（隐式模式，还需服务端 implicit_enabled=true）",
                    "en:Select one or more: authorization_code, password, client_credentials, refresh_token, implicit"})
    public String grant_types;

    @ModelField(list = true, required = true,
            name = {"授权范围", "en:Scope"},
            info = {"该客户端可申请的权限范围，多个用逗号或空格分隔；OIDC 需包含 openid，可选 profile/email，例如 openid profile email read",
                    "en:Scopes separated by comma or space; include openid for OIDC, e.g. openid profile email read"})
    public String scope;

    @ModelField(input_type = InputType.select, required = true,
            options = {"client_secret_basic", "client_secret_post", "none"},
            name = {"客户端认证方式", "en:Token Auth Method"},
            info = {"单选：client_secret_basic（HTTP Basic 传密钥，RFC 默认）、client_secret_post（表单传密钥）、"
                    + "none（公开客户端，授权码流程必须配合 PKCE）",
                    "en:One method: client_secret_basic (RFC default), client_secret_post, or none"})
    public String token_endpoint_auth_method = "client_secret_basic";

    @ModelField(input_type = InputType.bool,
            name = {"强制 PKCE", "en:Require PKCE"},
            info = {"开启后该客户端的授权码流程必须携带 code_challenge，推荐公开客户端开启",
                    "en:Require code_challenge for the authorization code flow"})
    public String require_pkce = "false";

    @ModelField(input_type = InputType.number,
            name = {"Access Token TTL", "en:Access Token TTL"},
            info = {"访问令牌有效期（秒），留空使用全局默认；客户端设置优先于全局",
                    "en:Access token lifetime in seconds; blank uses global default"})
    public String access_token_ttl;

    @ModelField(input_type = InputType.number,
            name = {"Refresh Token TTL", "en:Refresh Token TTL"},
            info = {"刷新令牌有效期（秒），留空使用全局默认；仅登记 refresh_token 授予时生效",
                    "en:Refresh token lifetime in seconds; blank uses global default"})
    public String refresh_token_ttl;

    @ModelField(list = true, input_type = InputType.bool,
            name = {"启用", "en:Enabled"},
            info = {"关闭后该客户端不能再申请令牌或发起授权", "en:Disabled clients cannot authenticate or authorize"})
    public String enabled = "true";

    private Store store;

    @Override
    public void start() {
        try {
            store = Store.get(getAppContext());
        } catch (Throwable e) {
            Logger logger = getAppContext().getService(Logger.class);
            if (logger != null) logger.error("failed to init oauth store", e);
        }
    }

    @Override
    public List<String[]> page(int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception {
        return store.page(Store.CLIENT_TABLE, "client_id", pageNum, pageSize, keyword(query, "client_id"), listFields);
    }

    @Override
    public int total(Map<String, String> query) {
        try {
            return store.count(Store.CLIENT_TABLE, "client_id", keyword(query, "client_id"));
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean contains(String id) {
        try {
            return store.exists(Store.CLIENT_TABLE, id);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Map<String, String> show(String id) throws Exception {
        Map<String, String> client = store.show(Store.CLIENT_TABLE, id);
        if (client != null) client.remove("client_secret"); // 不回传摘要，留空即保留原值
        return client;
    }

    @ModelAction(code = "add", icon = "Check", order = 1, add = true,
            name = {"保存", "en:Save"},
            info = {"保存新建的接入客户端；校验失败时直接返回可读错误", "en:Create the client; validation errors are returned to the form"})
    public void addClient(Request request) throws Exception {
        FormSupport.save(request, () -> add(FormSupport.collect(request, FORM_FIELDS)));
    }

    @ModelAction(code = "update", icon = "Check", order = 1, update = true,
            name = {"更新", "en:Update"},
            info = {"更新接入客户端；校验失败时直接返回可读错误", "en:Update the client; validation errors are returned to the form"})
    public void updateClient(Request request) throws Exception {
        FormSupport.save(request, () -> update(request.getId(), FormSupport.collect(request, FORM_FIELDS)));
    }

    @ModelAction(code = "guide", icon = "Document", order = 2, list = true,
            name = {"接入示例", "en:Integration Guide"},
            info = {"打开该客户端的接入示例页，包含端点、授权方式和可复制的 curl 示例",
                    "en:Open the integration guide for this client"})
    public void guide(Request request) throws Exception {
        String id = request.getParameter("id");
        Map<String, String> client = store.show(Store.CLIENT_TABLE, id);
        if (client == null) {
            request.getResponse().error("客户端不存在: " + id);
            return;
        }
        try {
            String path = "/oauth-server/guide?client_id="
                    + java.net.URLEncoder.encode(client.get("client_id"), "UTF-8");
            request.getResponse().data(path).message("已生成接入示例：" + path);
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private void add(Map<String, String> data) throws Exception {
        String clientId = data.get("client_id");
        if (Security.isEmpty(clientId)) throw new IllegalArgumentException("client_id 不能为空");
        if (Security.isEmpty(data.get("client_name"))) throw new IllegalArgumentException("client_name 不能为空");
        if (store.findClient(clientId) != null) throw new IllegalArgumentException("client_id 已存在: " + clientId);

        // 公开客户端（none）可以没有密钥，但列 NOT NULL，显式落空串
        data.putIfAbsent("client_secret", "");
        validate(data);
        if (Security.isEmpty(data.get("enabled"))) data.put("enabled", "true");
        if (Security.isEmpty(data.get("require_pkce"))) data.put("require_pkce", "false");
        store.insert(Store.CLIENT_TABLE, data);
    }

    private void update(String id, Map<String, String> data) throws Exception {
        Map<String, String> existing = store.show(Store.CLIENT_TABLE, id);
        if (existing == null) throw new IllegalArgumentException("客户端不存在: " + id);

        String clientId = data.get("client_id");
        if (!Security.isEmpty(clientId) && !clientId.equals(existing.get("client_id"))
                && store.findClient(clientId) != null) {
            throw new IllegalArgumentException("client_id 已存在: " + clientId);
        }
        if (Security.isEmpty(data.get("client_secret"))) data.remove("client_secret");

        Map<String, String> candidate = new LinkedHashMap<>(existing);
        candidate.putAll(data);
        if (Security.isEmpty(candidate.get("client_secret"))) {
            candidate.put("client_secret", existing.get("client_secret"));
        }
        validate(candidate);
        store.update(Store.CLIENT_TABLE, id, data);
    }

    @Override
    public void delete(String id) throws Exception {
        store.delete(Store.CLIENT_TABLE, id);
    }

    private static void validate(Map<String, String> data) {
        String clientSecret = data.get("client_secret");
        String authMethods = data.get("token_endpoint_auth_method");
        java.util.Set<String> methods = Security.split(authMethods);
        if (methods.isEmpty()) methods.addAll(Security.split(Store.DEFAULT_AUTH_METHOD));
        for (String method : methods) {
            if (!Security.split(AUTH_METHODS).contains(method)) {
                throw new IllegalArgumentException("不支持的客户端认证方式: " + method);
            }
        }
        boolean allowsNone = methods.contains("none");
        if (!allowsNone && Security.isEmpty(clientSecret)) {
            throw new IllegalArgumentException("client_secret_basic/post 认证方式必须设置 client_secret");
        }

        java.util.Set<String> grants = Security.split(data.get("grant_types"));
        if (grants.isEmpty()) throw new IllegalArgumentException("请至少选择一个授予类型");
        for (String grant : grants) {
            if (!Security.split(GRANTS).contains(grant)) throw new IllegalArgumentException("不支持的授予类型: " + grant);
        }
        if (allowsNone && grants.contains("client_credentials")) {
            throw new IllegalArgumentException("公开客户端（none）不能使用 client_credentials 授予");
        }

        java.util.Set<String> redirects = Security.splitRedirects(data.get("redirect_uri"));
        boolean redirectRequired = grants.contains("authorization_code") || grants.contains("implicit");
        if (redirectRequired && redirects.isEmpty()) {
            throw new IllegalArgumentException("授权码/隐式模式至少需要登记一个回调地址 redirect_uri");
        }
        for (String redirect : redirects) {
            String invalid = Security.validateRedirectUri(redirect);
            if (invalid != null) throw new IllegalArgumentException(invalid);
        }

        java.util.Set<String> scopes = Security.split(data.get("scope"));
        if (scopes.isEmpty()) throw new IllegalArgumentException("scope 不能为空");

        validateTtl(data.get("access_token_ttl"), "access_token_ttl");
        validateTtl(data.get("refresh_token_ttl"), "refresh_token_ttl");
    }

    private static void validateTtl(String value, String name) {
        if (Security.isEmpty(value)) return;
        try {
            if (Long.parseLong(value) <= 0) throw new IllegalArgumentException(name + " 必须大于 0");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " 必须是正整数");
        }
    }

    static String keyword(Map<String, String> query, String column) {
        if (query == null) return "";
        String keyword = query.get(column);
        return keyword == null ? "" : keyword.trim();
    }
}
