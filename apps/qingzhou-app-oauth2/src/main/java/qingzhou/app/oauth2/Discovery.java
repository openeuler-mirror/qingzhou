package qingzhou.app.oauth2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;

/**
 * OAuth 2.0（RFC 8414）与 OpenID Connect 1.0 授权服务器元数据。
 */
public class Discovery implements HttpHandler {
    private final Store store;
    private final Json json;
    private final Oidc oidc;
    private final boolean implicitEnabled;
    private final boolean oidcMetadata;
    private final List<String> scopes;

    public Discovery(Store store, Json json, Oidc oidc, boolean implicitEnabled,
                     String scopesSupported, boolean oidcMetadata) {
        this.store = store;
        this.json = json;
        this.oidc = oidc;
        this.implicitEnabled = implicitEnabled;
        this.oidcMetadata = oidcMetadata;
        Set<String> configured = new LinkedHashSet<>(Security.split(scopesSupported));
        configured.addAll(Oidc.oidcScopes());
        this.scopes = new ArrayList<>(configured);
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response) throws Exception {
        if (!"GET".equals(request.getMethod())) {
            Reply.methodNotAllowed(response, "GET");
            return;
        }

        String issuer = oidc.issuer(store, request);
        List<String> responseTypes = new ArrayList<>();
        responseTypes.add("code");
        if (implicitEnabled) {
            responseTypes.add("token");
            if (oidcMetadata) {
                responseTypes.add("id_token");
                responseTypes.add("id_token token");
            }
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("issuer", issuer);
        body.put("authorization_endpoint", issuer + "/authorize");
        body.put("token_endpoint", issuer + "/token");
        body.put("userinfo_endpoint", issuer + "/userinfo");
        body.put("introspection_endpoint", issuer + "/introspect");
        body.put("revocation_endpoint", issuer + "/revoke");
        body.put("response_types_supported", responseTypes);
        body.put("grant_types_supported", implicitEnabled
                ? Arrays.asList("authorization_code", "password", "client_credentials", "refresh_token", "implicit")
                : Arrays.asList("authorization_code", "password", "client_credentials", "refresh_token"));
        body.put("token_endpoint_auth_methods_supported",
                Arrays.asList("client_secret_basic", "client_secret_post", "none"));
        body.put("introspection_endpoint_auth_methods_supported",
                Arrays.asList("client_secret_basic", "client_secret_post", "none"));
        body.put("revocation_endpoint_auth_methods_supported",
                Arrays.asList("client_secret_basic", "client_secret_post", "none"));
        body.put("code_challenge_methods_supported", Arrays.asList("S256", "plain"));
        body.put("scopes_supported", scopes);
        if (oidcMetadata) {
            body.put("jwks_uri", issuer + "/jwks.json");
            body.put("response_modes_supported", Arrays.asList("query", "fragment"));
            body.put("subject_types_supported", Collections.singletonList("public"));
            body.put("id_token_signing_alg_values_supported", Collections.singletonList("RS256"));
            body.put("claims_supported", Oidc.claims());
        }
        Reply.send(response, json, body);
    }
}
