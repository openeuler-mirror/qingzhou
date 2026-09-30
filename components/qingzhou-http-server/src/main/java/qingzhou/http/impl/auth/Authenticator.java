package qingzhou.http.impl.auth;

import qingzhou.http.server.HttpRequest;

public interface Authenticator {
    AuthResult authenticate(HttpRequest request);
}
