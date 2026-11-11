package qingzhou.ai.mcp;

import java.util.Map;

import org.osgi.service.component.annotations.*;
import qingzhou.ai.Constants;
import qingzhou.ai.ToolCallInterceptionService;
import qingzhou.ai.ToolParameterParser;
import qingzhou.ai.ToolService;
import qingzhou.common.Configs;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.logger.Logger;

/**
 * MCP over Streamable HTTP（POST /mcp）：OSGi 生命周期、工具注册与异步响应收尾。
 * 协议收发见 {@link JsonRpc}，方法语义见 {@link McpMethods}，执行线程见 {@link WorkerPool}。
 */
@Component(configurationPid = "qingzhou-ai", configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {
                HttpHandler.HANDLE_PATH + "=/ai/mcp",
                HttpHandler.HANDLE_PATH_ABSOLUTE + "=true",
                HttpHandler.HANDLE_METHODS + "=POST"})
public class McpServer implements HttpHandler {
    @Reference
    private Json json;
    @Reference
    private Logger logger;
    @Reference
    private ToolCallInterceptionService interceptionService;
    @Reference
    private ToolParameterParser parameterParser;

    private final ToolRegistry toolRegistry = new ToolRegistry();
    private volatile WorkerPool workers;
    private volatile JsonRpc jsonRpc;

    @Activate
    public void init(Map<String, String> config) {
        Configs configs = new Configs(config);
        workers = new WorkerPool(configs);
        jsonRpc = new JsonRpc(json, logger, new McpMethods(toolRegistry, interceptionService, logger,
                configs.getOrDefault("qingzhou.version", "unknown")));
    }

    @Deactivate
    public void destroy() {
        try {
            if (!workers.terminate()) logger.warn("mcp workers did not terminate within 5s");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 停用路径恢复中断位，不阻断停用
        }
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {
        byte[] requestBody = httpRequest.getBody();
        long enqueuedAt = System.nanoTime();
        httpResponse.async(); // 工具执行可能长时间阻塞，响应由业务线程写并收尾
        if (workers.submit(() -> respond(httpRequest, httpResponse, requestBody, enqueuedAt))) return;

        logger.error("mcp worker pool is exhausted, request rejected");
        write(httpResponse, JsonRpc.failure(503, "Server busy"));
        httpResponse.finish();
    }

    // 业务线程执行：解析、分发与写响应；异步响应必须在此收尾
    private void respond(HttpRequest httpRequest, HttpResponse httpResponse, byte[] requestBody, long enqueuedAt) {
        try {
            if (workers.isExpired(enqueuedAt)) {
                logger.warn("mcp queued request expired");
                write(httpResponse, JsonRpc.failure(503, "Queued request expired"));
                return;
            }
            write(httpResponse, jsonRpc.dispatch(httpRequest, requestBody));
        } catch (Throwable t) { // 异步任务兜底：逃逸既无日志，又因漏写响应挂到空闲超时
            logger.error("mcp request failed", t);
            write(httpResponse, JsonRpc.failure(500, "Internal error"));
        } finally {
            httpResponse.finish(); // 异步响应由业务线程收尾，重复调用无副作用
        }
    }

    // 唯一的响应写点：写完不再补写，避免已提交响应后二次写入
    private void write(HttpResponse httpResponse, JsonRpc.Reply reply) {
        try {
            httpResponse.status(reply.status);
            if (reply.body != null) httpResponse.contentTypeJsonUtf8().send(json.toJson(reply.body));
        } catch (Exception e) {
            // 契约适配：HttpResponse 未暴露连接状态，按 emit 结果区分客户端断连与真实故障
            if (e instanceof IllegalStateException && e.getMessage() != null && e.getMessage().contains("FAIL_CANCELLED")) {
                logger.warn("mcp client disconnected");
            } else {
                logger.error("failed to write mcp response", e);
            }
        }
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE,
            target = "(" + ToolService.TOOL_LABEL + "=" + Constants.TOOL_LABEL_INVOKE_ACTION + ")", // 按服务属性过滤
            unbind = "unbindAiTool")
    public void bindAiTool(ToolService tool, Map<String, Object> properties) {
        toolRegistry.put(new ToolCache(tool, properties, parameterParser));
    }

    // 显式声明 unbind：动态多引用按服务实例增删
    public void unbindAiTool(ToolService tool) {
        toolRegistry.remove(tool);
    }
}
