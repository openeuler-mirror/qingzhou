package qingzhou.http.server;

public interface HttpServer {
    void registerHttpHandler(HttpHandler httpHandler, String handlePath);

    /**
     * 注册并限定允许的 HTTP 方法；不传 methods 表示允许全部方法。
     */
    void registerHttpHandler(HttpHandler httpHandler, String handlePath, boolean noAuth, String... methods);

    /**
     * 注册并指定是否允许不透明来源（浏览器发送的字面量 Origin: null）执行写请求。
     * 仅应授予不依赖 Cookie 会话的公开端点；默认实现保持拒绝。
     */
    void registerHttpHandler(HttpHandler httpHandler, String handlePath, boolean noAuth, boolean allowOpaqueOrigin, String... methods);

    void unregisterHttpHandler(HttpHandler httpHandler);

    /**
     * 实际监听端口：配置 port=0 时由操作系统分配，经此读取真实端口（未启动时抛 IllegalStateException）
     */
    int getPort();

    /**
     * 实际生效的全局路径前缀（已归一化，配置 "/" 返回空串）：跨节点回连 URL 须以此为基准，避免配置省略时默认值漂移。
     */
    String getContextPath();
}
