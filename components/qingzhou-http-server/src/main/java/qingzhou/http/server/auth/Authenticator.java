package qingzhou.http.server.auth;

import qingzhou.http.server.HttpRequest;

public interface Authenticator {
    AuthResult authenticate(HttpRequest request);
}
