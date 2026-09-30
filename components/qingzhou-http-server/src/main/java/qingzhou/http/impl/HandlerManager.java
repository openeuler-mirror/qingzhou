package qingzhou.http.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.annotations.*;
import qingzhou.http.impl.util.PendingLogs;
import qingzhou.http.server.HttpHandler;
import qingzhou.logger.Logger;

/**
 * 路由表：handler 仅在启动期注册、停止期解绑，与请求分发不存在并发竞争，故无需额外索引或同步。
 */
@Component(service = HandlerManager.class)
public class HandlerManager {
    @Reference
    private Logger logger;

    private final PendingLogs pendingLogs = new PendingLogs();
    private final Map<String, HandlerEntry> handlerMap = new ConcurrentHashMap<>();

    /**
     * 注册路径对应的 handler 与免认证标记。
     * 标记随路径存储而非随 handler 实例：同一实例注册到多条路径时，各自的认证要求互不干扰。
     */
    public static final class HandlerEntry {
        public final HttpHandler handler;
        public final boolean noAuth;

        HandlerEntry(HttpHandler handler, boolean noAuth) {
            this.handler = handler;
            this.noAuth = noAuth;
        }
    }

    @Activate
    public void activate() {
        pendingLogs.replay(logger);
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE,
            unbind = "removeHttpHandler")
    public void addHttpHandler(HttpHandler httpHandler, Map<String, String> properties, ServiceReference<HttpHandler> reference) {
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
        handlerMap.put(path, new HandlerEntry(httpHandler, isNoAuth));

        // 免认证路径对外可直接访问，来源 bundle 须留痕以便审计
        String source = reference != null ? reference.getBundle().getSymbolicName() : component;
        pendingLogs.log(logger, "registered: [" + path + "] by [" + source + "]" + (isNoAuth ? " (no auth)" : ""));
        if (isNoAuth) {
            pendingLogs.warn(logger, "no-auth route: [" + path + "] is publicly reachable, declared by [" + source + "]");
        }
    }

    // 注册专用：父子路径任一方向重叠即冲突，避免二者在分发时互相遮蔽
    private String conflict(String checkPath) {
        for (String existsPath : handlerMap.keySet()) {
            if (checkPath.startsWith(existsPath) || existsPath.startsWith(checkPath)) return existsPath;
        }
        return null;
    }

    // 补尾部斜杠：使 /a 只匹配 /a/...，不会误匹配 /abc
    private String withTrailingSlash(String path) {
        return path.endsWith("/") ? path : path + "/";
    }

    // 方法名由 addHttpHandler 按 OSGi DS 规范推导（add -> remove），不可随意改名
    public void removeHttpHandler(HttpHandler httpHandler) {
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
