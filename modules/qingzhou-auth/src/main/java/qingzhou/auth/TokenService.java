package qingzhou.auth;

public interface TokenService {
    String createToken(String user);

    String verifyToken(String token);
}
