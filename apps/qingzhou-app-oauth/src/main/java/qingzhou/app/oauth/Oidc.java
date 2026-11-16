package qingzhou.app.oauth;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import qingzhou.http.server.HttpRequest;
import qingzhou.json.Json;

/**
 * OpenID Connect 1.0 的 RSA 签名、ID Token 与 JWKS 支持。
 * 默认每次启动生成 RSA 密钥；生产环境可通过 oidc_private_key 固定密钥。
 */
final class Oidc {
    private static final String PREFIX = "/oauth-server";
    private static final String ALG = "RS256";

    private final Json json;
    private final KeyPair keyPair;
    private final String kid;

    Oidc(Json json, Map<String, String> config) throws Exception {
        this.json = json;
        String pem = config.get("oidc_private_key");
        if (Security.isEmpty(pem)) {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            this.keyPair = generator.generateKeyPair();
        } else {
            this.keyPair = fromPem(pem);
        }
        String configuredKid = config.get("oidc_key_id");
        this.kid = Security.isEmpty(configuredKid)
                ? base64Url(sha256(keyPair.getPublic().getEncoded())).substring(0, 16)
                : configuredKid.trim();
    }

    String issuer(Store store, HttpRequest request) {
        String issuer = store.getIssuer();
        if (!Security.isEmpty(issuer)) {
            return issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;
        }
        String host = request.getHeader("Host");
        return request.getScheme() + "://" + (Security.isEmpty(host) ? "localhost" : host) + PREFIX;
    }

    String idToken(HttpRequest request, Store store, Map<String, String> client, String userName,
                   String scope, String nonce, String accessToken, long authTime) throws Exception {
        long now = Store.now();
        String clientId = client.get("client_id");
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", ALG);
        header.put("typ", "JWT");
        header.put("kid", kid);

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", issuer(store, request));
        claims.put("sub", userName);
        claims.put("aud", clientId);
        claims.put("exp", now + store.accessTtl(client));
        claims.put("iat", now);
        claims.put("auth_time", authTime > 0 ? authTime : now);
        if (!Security.isEmpty(nonce)) claims.put("nonce", nonce);
        if (!Security.isEmpty(accessToken)) claims.put("at_hash", atHash(accessToken));
        Set<String> scopes = Security.split(scope);
        if (scopes.contains("profile") || scopes.contains("email")) {
            Map<String, String> user = store.findUser(userName);
            if (user != null) {
                if (scopes.contains("profile")) {
                    putIfPresent(claims, "name", user.get("nickname"));
                    putIfPresent(claims, "preferred_username", user.get("username"));
                    putIfPresent(claims, "role", user.get("rolename"));
                    putIfPresent(claims, "roles", user.get("rolename"));
                }
                if (scopes.contains("email")) putIfPresent(claims, "email", user.get("email"));
            }
        }

        String encodedHeader = base64Url(json.toJson(header).getBytes(StandardCharsets.UTF_8));
        String encodedClaims = base64Url(json.toJson(claims).getBytes(StandardCharsets.UTF_8));
        String signingInput = encodedHeader + "." + encodedClaims;
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
        return signingInput + "." + base64Url(signature.sign());
    }

    Map<String, Object> jwks() {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        Map<String, Object> jwk = new LinkedHashMap<>();
        jwk.put("kty", "RSA");
        jwk.put("use", "sig");
        jwk.put("alg", ALG);
        jwk.put("kid", kid);
        jwk.put("n", base64Url(unsigned(publicKey.getModulus())));
        jwk.put("e", base64Url(unsigned(publicKey.getPublicExponent())));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("keys", Collections.singletonList(jwk));
        return result;
    }

    static List<String> oidcScopes() {
        return Arrays.asList("openid", "profile", "email");
    }

    static List<String> claims() {
        List<String> claims = new ArrayList<>(Arrays.asList(
                "sub", "iss", "aud", "exp", "iat", "auth_time", "nonce", "at_hash",
                "name", "preferred_username", "email", "role", "roles"));
        return claims;
    }

    private static KeyPair fromPem(String pem) throws Exception {
        String normalized = pem.replace("\\n", "\n")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getMimeDecoder().decode(normalized);
        PrivateKey privateKey = KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(der));
        if (!(privateKey instanceof RSAPrivateCrtKey)) {
            throw new IllegalArgumentException("oidc_private_key 必须是 PKCS#8 RSA 私钥");
        }
        RSAPrivateCrtKey rsa = (RSAPrivateCrtKey) privateKey;
        PublicKey publicKey = KeyFactory.getInstance("RSA")
                .generatePublic(new RSAPublicKeySpec(rsa.getModulus(), rsa.getPublicExponent()));
        return new KeyPair(publicKey, privateKey);
    }

    private static void putIfPresent(Map<String, Object> claims, String key, String value) {
        if (!Security.isEmpty(value)) claims.put(key, value);
    }

    private static String atHash(String accessToken) throws Exception {
        byte[] digest = sha256(accessToken.getBytes(StandardCharsets.US_ASCII));
        return base64Url(Arrays.copyOf(digest, digest.length / 2));
    }

    private static byte[] sha256(byte[] value) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(value);
    }

    private static byte[] unsigned(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            return Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return bytes;
    }

    private static String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
