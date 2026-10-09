package qingzhou.http.server.impl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.annotations.*;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.util.PendingLogs;
import qingzhou.logger.Logger;

/**
 * 路由表：注册与解绑可能和请求分发并发；写操作串行化，读取使用并发 Map 快照。
 */
@Component(service = HandlerManager.class)
public class HandlerManager {
    @Reference
    private Logger logger;

    private final PendingLogs pendingLogs = new PendingLogs();
    private final Map<String, HandlerEntry> handlerMap = new ConcurrentHashMap<>();

    /**
     * 注册路径对应的 handler、免认证标记与允许的方法。
     * 标记随路径存储而非随 handler 实例：同一实例注册到多条路径时，各自的认证要求互不干扰。
     * 方法列表为空表示允许全部方法；非空时由框架在认证前拦截。
     */
    public static final class HandlerEntry {
        public final HttpHandler handler;
        public final boolean noAuth;
        public final List<String> methods; // 允许的方法；空表示全部允许

        HandlerEntry(HttpHandler handler, boolean noAuth, List<String> methods) {
            this.handler = handler;
            this.noAuth = noAuth;
            this.methods = methods;
        }
    }

    @Activate
    public void activate() {
        pendingLogs.replay(logger);
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE,
            unbind = "removeHttpHandler")
    public synchronized void addHttpHandler(HttpHandler httpHandler, Map<String, String> properties, ServiceReference<HttpHandler> reference) {
        String originPath = properties.get(HttpHandler.HANDLE_PATH);

        String component = properties.get(ComponentConstants.COMPONENT_NAME);
        if (component == null) component = "@App";

        if (originPath == null || originPath.trim().isEmpty()) {
            throw new IllegalArgumentException(HttpHandler.HANDLE_PATH + " of [" + component + "] cannot be empty");
        }
        String path = originPath.trim();

        if (!path.startsWith("/")) {
            throw new IllegalArgumentException(HttpHandler.HANDLE_PATH + " of [" + component + "] must start with /, got: " + path);
        }
        path = withTrailingSlash(path); // 统一以尾斜杠存储，匹配时无需逐个 key 再做转换

        boolean isAbsolute = Boolean.parseBoolean(properties.get(HttpHandler.HANDLE_PATH_ABSOLUTE));
        if (!isAbsolute) {
            String prefix = reference.getBundle().getSymbolicName();
            prefix = prefix.replace("qingzhou-", "");
            path = "/" + prefix + path;
        }
        if (path.equals("/")) {
            throw new IllegalArgumentException(HttpHandler.HANDLE_PATH + " of [" + component + "] cannot be root path");
        }

        String conflict = conflict(path);
        if (conflict != null) {
            throw new IllegalArgumentException(HttpHandler.HANDLE_PATH + "(" + originPath + ") of [" + component
                    + "] conflicts: " + conflict + " of [" + handlerMap.get(conflict).handler.getClass().getName() + "]");
        }

        boolean isNoAuth = Boolean.parseBoolean(properties.get(HttpHandler.HANDLE_NO_AUTH));
        List<String> methods = parseMethods(properties.get(HttpHandler.HANDLE_METHODS), component);
        handlerMap.put(path, new HandlerEntry(httpHandler, isNoAuth, methods));

        // 免认证路径对外可直接访问，来源 bundle 须留痕以便审计
        String source = reference != null ? reference.getBundle().getSymbolicName() : component;
        pendingLogs.log(logger, "registered: [" + path + "] by [" + source + "]"
                + (methods.isEmpty() ? "" : " methods=" + methods) + (isNoAuth ? " (no auth)" : ""));
        if (isNoAuth) {
            pendingLogs.warn(logger, "no-auth route: [" + path + "] is publicly reachable, declared by [" + source + "]");
        }
    }

    // 注册专用：方法须属框架白名单，非法值在注册期即暴露，避免运行期静默放行
    private static List<String> parseMethods(String declared, String component) {
        if (declared == null) return Collections.emptyList(); // 未声明：允许全部白名单方法
        List<String> methods = new ArrayList<>();
        for (String name : declared.split(",")) {
            String method = name.trim().toUpperCase(Locale.ROOT);
            if (method.isEmpty()) continue;
            if (!RequestGates.isSupportedMethod(method)) {
                throw new IllegalArgumentException(HttpHandler.HANDLE_METHODS + " of [" + component
                        + "] contains unsupported method: " + method);
            }
            methods.add(method);
        }
        // 显式声明却解析为空（如 "," 或全空白）属配置错误：fail-fast 而非静默放行
        if (methods.isEmpty()) {
            throw new IllegalArgumentException(HttpHandler.HANDLE_METHODS + " of [" + component + "] is empty: " + declared);
        }
        return methods;
    }

    // 注册专用：父子路径任一方向重叠即冲突，避免二者在分发时互相遮蔽
    private String conflict(String checkPath) {
        for (String existsPath : handlerMap.keySet()) {
            if (checkPath.startsWith(existsPath) || existsPath.startsWith(checkPath)) return existsPath;
        }
        return null;
    }

    // 补尾部斜杠：使 /a 只匹配 /a/...，不会误匹配 /abc
    private static String withTrailingSlash(String path) {
        return path.endsWith("/") ? path : path + "/";
    }

    // 方法名由 addHttpHandler 按 OSGi DS 规范推导（add -> remove），不可随意改名
    public synchronized void removeHttpHandler(HttpHandler httpHandler) {
        // 同一实例可注册到多条路径，须全部移除：残留条目（尤其 noAuth）会继续对外服务
        List<String> removedPaths = new ArrayList<>();
        handlerMap.entrySet().removeIf(e -> {
            if (!Objects.equals(e.getValue().handler, httpHandler)) return false;
            removedPaths.add(e.getKey());
            return true;
        });
        if (removedPaths.isEmpty()) return;

        if (logger != null) logger.info("unregistered: " + removedPaths);
    }

    /**
     * 分发专用：只按「请求路径以已注册路径为前缀」匹配，并取最长者。
     * 反向匹配（已注册路径以请求路径为前缀）会让后代 handler 服务祖先请求，
     * 例如请求 / 命中 /ai/chat/config、请求 /ai/chat 命中 /ai/chat/stream，故不做。
     * 直接返回整个条目：免认证标记必须随条目返回，否则只能按 handler 实例判断，会被多路径注册串味。
     */
    HandlerEntry findHandler(String checkPath) {
        String request = withTrailingSlash(checkPath);
        HandlerEntry matched = null;
        int matchedLength = 0;
        for (Map.Entry<String, HandlerEntry> entry : handlerMap.entrySet()) {
            String existsPath = entry.getKey();
            if (existsPath.length() > matchedLength && request.startsWith(existsPath)) {
                matched = entry.getValue();
                matchedLength = existsPath.length();
            }
        }
        return matched;
    }
}
