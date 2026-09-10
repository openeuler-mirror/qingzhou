package qingzhou.http.server;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public interface AuthResult {
    String AUTH_ROLES_ATTRIBUTE = "auth.roles";
    String AUTH_PRINCIPAL_ATTRIBUTE = "auth.principal.username";
    @Deprecated
    String AUTH_PRINCIPAL_USERNAME_ATTRIBUTE = AUTH_PRINCIPAL_ATTRIBUTE;

    default Set<String> getRoles() { return Collections.emptySet(); }

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
        return pass(user, Collections.emptySet());
    }

    static AuthResult pass(String user, Set<String> roles) {
        Set<String> copy = roles == null || roles.isEmpty() ? Collections.emptySet()
                : Collections.unmodifiableSet(new HashSet<>(roles));
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
            public Set<String> getRoles() { return copy; }
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
