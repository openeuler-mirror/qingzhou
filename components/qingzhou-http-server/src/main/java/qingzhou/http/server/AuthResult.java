package qingzhou.http.server;

public interface AuthResult {
    String AUTH_PRINCIPAL_ATTRIBUTE = "auth.principal";
    String AUTH_ROLES_ATTRIBUTE = "auth.roles";

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

    // 认证角色
    default String[] getRoles() {
        return null;
    }

    static AuthResult pass(String user, String[] roles) {
        return new AuthResult() {
            @Override
            public Status status() {
                return Status.PASS;
            }

            @Override
            public Object getPrincipal() {
                return user;
            }

            @Override
            public String[] getRoles() {
                return roles;
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
