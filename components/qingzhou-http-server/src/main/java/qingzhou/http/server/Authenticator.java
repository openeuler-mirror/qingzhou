package qingzhou.http.server;

public interface Authenticator {
    AuthResult authenticate(HttpRequest request);
}
