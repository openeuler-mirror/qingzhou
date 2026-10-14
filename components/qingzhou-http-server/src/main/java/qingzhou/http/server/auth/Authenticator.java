package qingzhou.http.server.auth;

import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;

public interface Authenticator {
    AuthResult authenticate(HttpRequest request, HttpResponse response);
}
