package qingzhou.http.server;

public interface AuthResult {
    String AUTH_PRINCIPAL_USERNAME_ATTRIBUTE = "auth.principal.username";

    enum Status {PASS, REJECT}

    Status status();

    // 认证主体
    default Object getPrincipal() {
        return null;
    }

    // 拒绝原因
    default String getReason() {
        return null;
    }

    static AuthResult pass(String user) {
        return new AuthResult() {
            @Override
            public Status status() {
                return Status.PASS;
            }

            @Override
            public Object getPrincipal() {
                return user;
            }
        };
    }

    static AuthResult reject(String reason) {
        return new AuthResult() {
            @Override
            public Status status() {
                return Status.REJECT;
            }

            @Override
            public String getReason() {
                return reason;
            }
        };
    }
}
