package qingzhou.ai;

import java.util.Map;

public interface ToolInterceptor {
    String intercept(String toolName, Map<String, Object> argsMap, InterceptorContext context);

    interface InterceptorContext {
        // 认证主体
        default String getPrincipal() {
            return null;
        }

        // 认证角色
        default String[] getRoles() {
            return null;
        }
    }
}
