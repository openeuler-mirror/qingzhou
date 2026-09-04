package qingzhou.http.server;

public interface AuthResult {
    // 认证结果状态，四者互斥且必有其一
    enum Status {PASS, REJECT, CHALLENGE, MISSING}

    Status status();

    // 拒绝原因
    default String getReason() {
        return null;
    }

    // 重定向地址
    default String getLocation() {
        return null;
    }

    // 认证主体
    default Object getPrincipal() {
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

    static AuthResult missing() {
        return () -> Status.MISSING;
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

    static AuthResult challenge(String location) {
        return new AuthResult() {
            @Override
            public Status status() {
                return Status.CHALLENGE;
            }

            @Override
            public String getLocation() {
                return location;
            }
        };
    }
}
