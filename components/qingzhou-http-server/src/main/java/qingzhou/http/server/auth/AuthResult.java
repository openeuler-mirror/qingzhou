package qingzhou.http.server.auth;

public interface AuthResult {
    // REJECT：已出示凭据但校验失败；NO_CREDENTIAL：未出示凭据（匿名访问），两者由认证框架区别对待（后者不计入爆破锁定）
    enum Status {PASS, REJECT, NO_CREDENTIAL}

    Status status();

    // 认证主体
    default String getPrincipal() {
        return null;
    }

    // 认证角色
    default String[] getRoles() {
        return null;
    }

    // 拒绝原因
    default String getReason() {
        return null;
    }

    static AuthResult pass(String user, String[] roles) {
        String[] copiedRoles = roles == null ? null : roles.clone(); // 防御性拷贝：外部事后修改不影响认证角色
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
                return copiedRoles == null ? null : copiedRoles.clone();
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

    static AuthResult noCredential(String reason) {
        return new AuthResult() {
            @Override
            public Status status() {
                return Status.NO_CREDENTIAL;
            }

            @Override
            public String getReason() {
                return reason;
            }
        };
    }
}
