package qingzhou.app.oauth;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import qingzhou.http.server.HttpRequest;

/**
 * OAuth 2.0 协议安全工具：客户端认证、回调地址、scope、PKCE、随机串与页面转义。
 */
final class Security {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String BASIC = "basic";

    private Security() {
    }

    static final class ClientAuth {
        final String clientId;
        final String secret;
        final String method;
        final boolean malformed;

        ClientAuth(String clientId, String secret, String method, boolean malformed) {
            this.clientId = clientId;
            this.secret = secret;
            this.method = method;
            this.malformed = malformed;
        }
    }

    static ClientAuth clientAuth(HttpRequest request) {
        String authorization = request.getHeader("Authorization");
        String clientId = request.getParameter("client_id");
        String secret = request.getParameter("client_secret");

        if (!isEmpty(authorization)) {
            String trimmed = authorization.trim();
            if (!trimmed.toLowerCase(Locale.ROOT).startsWith(BASIC + " ")) {
                return new ClientAuth(clientId, null, null, true);
            }
            if (!isEmpty(secret)) return new ClientAuth(clientId, null, null, true); // 两种凭据来源同时出现，拒绝歧义

            String decoded;
            try {
                decoded = new String(Base64.getDecoder().decode(trimmed.substring(BASIC.length()).trim()),
                        StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                return new ClientAuth(clientId, null, null, true);
            }
            int split = decoded.indexOf(':');
            if (split < 0) return new ClientAuth(clientId, null, null, true);

            String basicId = urlDecode(decoded.substring(0, split));
            String basicSecret = urlDecode(decoded.substring(split + 1));
            if (!isEmpty(clientId) && !clientId.equals(basicId)) return new ClientAuth(clientId, null, null, true);
            return new ClientAuth(basicId, basicSecret, "client_secret_basic", false);
        }

        if (!isEmpty(clientId) && !isEmpty(secret)) {
            return new ClientAuth(clientId, secret, "client_secret_post", false);
        }
        return new ClientAuth(clientId, null, "none", false);
    }

    static boolean clientAuthAllowed(Store store, Map<String, String> client, ClientAuth auth) {
        if (auth == null || auth.malformed || client == null || !Store.isEnabled(client)) return false;
        if (isEmpty(auth.clientId) || !auth.clientId.equals(client.get("client_id"))) return false;

        Set<String> methods = authMethods(client);
        if ("none".equals(auth.method)) {
            return methods.contains("none");
        }
        if (!methods.contains(auth.method)) return false;
        return store.verifySecret(auth.secret, client.get("client_secret"));
    }

    static Set<String> authMethods(Map<String, String> client) {
        Set<String> methods = split(client == null ? null : client.get("token_endpoint_auth_method"));
        if (methods.isEmpty()) methods.addAll(split(Store.DEFAULT_AUTH_METHOD));
        return methods;
    }

    static final class Redirect {
        final String uri;
        final String error;

        Redirect(String uri, String error) {
            this.uri = uri;
            this.error = error;
        }
    }

    static Redirect resolveRedirect(String requested, String registered) {
        Set<String> uris = splitRedirects(registered);
        if (uris.isEmpty()) return new Redirect(null, "客户端未登记 redirect_uri");

        for (String uri : uris) {
            String invalid = validateRedirectUri(uri);
            if (invalid != null) return new Redirect(null, invalid);
        }

        if (isEmpty(requested)) {
            if (uris.size() == 1) return new Redirect(uris.iterator().next(), null);
            return new Redirect(null, "客户端登记了多个 redirect_uri，必须显式指定");
        }
        String value = requested.trim();
        return uris.contains(value) ? new Redirect(value, null) : new Redirect(null, "redirect_uri 与登记值不一致");
    }

    static String validateRedirectUri(String uri) {
        if (isEmpty(uri)) return "redirect_uri 不能为空";
        String lower = uri.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) return "redirect_uri 仅支持 http/https 协议";
        if (uri.indexOf('#') >= 0) return "redirect_uri 不能包含 fragment";
        if (uri.indexOf('\r') >= 0 || uri.indexOf('\n') >= 0
                || lower.contains("%0d") || lower.contains("%0a")) {
            return "redirect_uri 含非法字符"; // 防止 CRLF 注入 Location 响应头
        }
        return null;
    }

    static String validateScope(String scope, String registered) {
        if (isEmpty(scope)) return null;

        Set<String> allowed = split(registered);
        for (String item : split(scope)) {
            if (!allowed.contains(item)) return "scope 超出客户端登记范围：" + item;
        }
        return null;
    }

    static String scopeOrDefault(String requested, String registered, String fallback) {
        if (!isEmpty(requested)) return requested.trim();
        if (!isEmpty(registered)) return registered.trim();
        return fallback;
    }

    static boolean grantAllowed(Map<String, String> client, String grantType) {
        return !isEmpty(grantType) && split(client.get("grant_types")).contains(grantType);
    }

    static boolean isPkceRequired(Map<String, String> client, boolean globalRequired) {
        if (globalRequired) return true;
        String value = client.get("require_pkce");
        return value != null && ("true".equalsIgnoreCase(value) || "1".equals(value));
    }

    static String validatePkce(String challenge, String method, boolean required) {
        if (isEmpty(challenge)) return required ? "该客户端要求使用 PKCE（code_challenge）" : null;
        if (challenge.length() < 43 || challenge.length() > 128 || !matchesPkceChars(challenge)) {
            return "code_challenge 格式非法";
        }
        String value = isEmpty(method) ? "plain" : method;
        if (!"plain".equals(value) && !"S256".equals(value)) return "仅支持 code_challenge_method=plain/S256";
        return null;
    }

    static boolean verifyPkce(String verifier, String challenge, String method) {
        if (isEmpty(challenge)) return true;
        if (isEmpty(verifier) || verifier.length() < 43 || verifier.length() > 128 || !matchesPkceChars(verifier)) {
            return false;
        }
        if ("plain".equals(method) || isEmpty(method)) return verifier.equals(challenge);
        if (!"S256".equals(method)) return false;
        return base64Url(sha256(verifier)).equals(challenge);
    }

    static String randomToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String escapeHtml(String value) {
        if (value == null) return "";

        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            switch (value.charAt(i)) {
                case '&':
                    escaped.append("&amp;");
                    break;
                case '<':
                    escaped.append("&lt;");
                    break;
                case '>':
                    escaped.append("&gt;");
                    break;
                case '"':
                    escaped.append("&quot;");
                    break;
                case '\'':
                    escaped.append("&#39;");
                    break;
                default:
                    escaped.append(value.charAt(i));
            }
        }
        return escaped.toString();
    }

    static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    static Set<String> split(String value) {
        Set<String> items = new LinkedHashSet<>();
        if (value == null) return items;

        for (String item : value.trim().split("[,\\s]+")) {
            if (!item.isEmpty()) items.add(item);
        }
        return items;
    }

    static Set<String> splitRedirects(String value) {
        Set<String> items = new LinkedHashSet<>();
        if (value == null) return items;
        for (String item : value.split("[,\\r\\n]+")) {
            String uri = item.trim();
            if (!uri.isEmpty()) items.add(uri);
        }
        return items;
    }

    static String urlDecode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            return value;
        }
    }

    private static boolean matchesPkceChars(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean allowed = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                    || c == '-' || c == '.' || c == '_' || c == '~';
            if (!allowed) return false;
        }
        return true;
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
