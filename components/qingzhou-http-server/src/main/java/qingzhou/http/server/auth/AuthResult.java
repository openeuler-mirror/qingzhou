package qingzhou.http.server.auth;

public interface AuthResult {
    enum Status {PASS, REJECT}

    Status status();

    // 拒绝原因
    default String getReason() {
        return null;
    }

    // 认证主体
    default String getPrincipal() {
        return null;
    }

    // 认证角色
    default String[] getRoles() {
        return null;
    }

    // 认证通过时需下发的 Set-Cookie 值（如活跃会话滑动续期重签令牌）；无则 null
    default String getSetCookie() {
        return null;
    }

    static AuthResult pass(String user, String[] roles) {
        return pass(user, roles, null);
    }

    static AuthResult pass(String user, String[] roles, String setCookie) {
        return new AuthResult() {
            @Override
            public Status status() {
                return Status.PASS;
            }

            @Override
            public String getPrincipal() {
                return user;
            }

            @Override
            public String[] getRoles() {
                return roles;
            }

            @Override
            public String getSetCookie() {
                return setCookie;
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
