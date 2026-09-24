package qingzhou.http.server;

public interface AuthResult {
    /**
     * PASS 放行；REJECT 拒绝；ABSTAIN 弃权——本认证器不适用于该请求，交由后续认证器判定。
     */
    enum Status {PASS, REJECT, ABSTAIN}

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

    static AuthResult pass(String user, String[] roles) {
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

    // 弃权：凭据类型不匹配等「本认证器说了不算」的场景，返回 null 与返回本值等价
    static AuthResult abstain() {
        return () -> Status.ABSTAIN;
    }
}
