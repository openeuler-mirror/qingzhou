package qingzhou.ai.mcp;

import java.nio.charset.StandardCharsets;
import java.util.*;

import qingzhou.http.server.HttpRequest;
import qingzhou.json.Json;
import qingzhou.logger.Logger;

/**
 * JSON-RPC 2.0 传输层：报文解析、批处理、请求校验与错误装配。
 * MCP 方法语义由 {@link McpMethods} 注入，本类不感知具体方法；除「无权调用」在非批处理下
 * 同步回 403（客户端据此触发重新认证）外，一切失败都以 JSON-RPC error 回给对端。
 */
class JsonRpc {
    private static final String VERSION = "2.0";

    private final Json json;
    private final Logger logger;
    private final McpMethods mcpMethods;

    JsonRpc(Json json, Logger logger, McpMethods mcpMethods) {
        this.json = json;
        this.logger = logger;
        this.mcpMethods = mcpMethods;
    }

    Reply dispatch(HttpRequest request, byte[] body) {
        Object message;
        try {
            message = json.fromJson(new String(body, StandardCharsets.UTF_8), Object.class);
        } catch (Exception e) {
            return new Reply(400, error(null, RpcException.PARSE_ERROR, "Parse error"));
        }

        boolean isBatch = message instanceof List;
        List<?> envelopes = isBatch ? (List<?>) message : Collections.singletonList(message);
        if (envelopes.isEmpty()) return new Reply(400, error(null, RpcException.INVALID_REQUEST, "Invalid Request"));

        List<Object> replies = new ArrayList<>(envelopes.size());
        int status = 200;
        for (Object envelope : envelopes) {
            Reply reply = replyTo(envelope, request, isBatch);
            if (reply == null) continue; // notification：不产生响应
            status = Math.max(status, reply.status);
            replies.add(reply.body);
        }
        if (replies.isEmpty()) return new Reply(202, null); // 全部为 notification：无内容可回
        return new Reply(status, isBatch ? replies : replies.get(0));
    }

    @SuppressWarnings("unchecked") // JSON 对象
    private Reply replyTo(Object envelope, HttpRequest request, boolean isBatch) {
        if (!(envelope instanceof Map)) return invalid(null);

        Map<String, Object> message = (Map<String, Object>) envelope;
        Object id = message.get("id");
        Object method = message.get("method");
        boolean idValid = validId(id);
        if (!VERSION.equals(message.get("jsonrpc")) || !(method instanceof String) || !idValid) {
            return invalid(idValid ? id : null); // id 不可回显时按规范回 null
        }
        if (!message.containsKey("id")) return null; // notification：无 id 不回
        if (isBatch && "initialize".equals(method)) {
            return new Reply(200, error(id, RpcException.INVALID_REQUEST, "initialize must not be batched"));
        }

        try {
            return new Reply(200, result(id, mcpMethods.invoke((String) method, message.get("params"), request)));
        } catch (RpcException e) {
            return new Reply(!isBatch && e.code == RpcException.FORBIDDEN ? 403 : 200,
                    error(id, e.code, e.getMessage(), e.data));
        } catch (Exception e) {
            logger.error("mcp " + method + " failed", e);
            return new Reply(200, error(id, RpcException.INTERNAL_ERROR, "Internal error"));
        }
    }

    // 无 id 的失败响应：解析或入队阶段失败，无从得知请求 id
    static Reply failure(int status, String message) {
        return new Reply(status, error(null, RpcException.INTERNAL_ERROR, message));
    }

    private static Reply invalid(Object id) {
        return new Reply(200, error(id, RpcException.INVALID_REQUEST, "Invalid Request"));
    }

    // JSON-RPC id 只接受 String、整数与有限整数值浮点；非有限数值回显会写出非法 JSON
    private static boolean validId(Object id) {
        if (id == null || id instanceof String) return true;
        if (id instanceof Double || id instanceof Float) {
            double value = ((Number) id).doubleValue();
            return Double.isFinite(value) && value == Math.rint(value);
        }
        return id instanceof Number; // Integer/Long/BigInteger 等
    }

    private static Map<String, Object> result(Object id, Object result) {
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", VERSION);
        response.put("id", id);
        response.put("result", result);
        return response;
    }

    private static Map<String, Object> error(Object id, int code, String message) {
        return error(id, code, message, null);
    }

    private static Map<String, Object> error(Object id, int code, String message, Object data) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);
        if (data != null) error.put("data", data);

        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", VERSION);
        response.put("id", id);
        response.put("error", error);
        return response;
    }

    static final class Reply {
        final int status;
        final Object body; // null 表示 202 无体

        Reply(int status, Object body) {
            this.status = status;
            this.body = body;
        }
    }
}
