package qingzhou.ai.impl;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import qingzhou.ai.ToolInterceptor;

/**
 * 工具调用的统一授权闸门：所有工具调用方（McpServer、AiChat）必须经此执行授权拦截，
 * 避免任一调用路径绕过 {@link ToolInterceptor} 角色校验。
 */
@Component(service = InterceptToolInvocation.class)
public class InterceptToolInvocation {
    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    private final Set<ToolInterceptor> interceptors = ConcurrentHashMap.newKeySet();

    /**
     * 集中执行工具调用前的授权拦截。
     *
     * @return null 表示放行；非 null 为拒绝原因，由调用方作为错误回传。
     */
    public String intercept(String toolName, Map<String, Object> args, String principal, String[] roles) {
        if (interceptors.isEmpty()) {
            return "授权拦截器未就绪，暂不支持工具调用！";
        }

        ToolInterceptor.InterceptorContext context = new ToolInterceptor.InterceptorContext() {
            @Override
            public String getPrincipal() {
                return principal;
            }

            @Override
            public String[] getRoles() {
                return roles;
            }
        };
        for (ToolInterceptor interceptor : interceptors) {
            String denied = interceptor.intercept(toolName, args, context);
            if (denied != null) return denied;
        }
        return null;
    }
}
