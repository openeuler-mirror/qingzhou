package qingzhou.ai.memory;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.http.server.AuthResult;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.logger.Logger;

@Component(property = HttpHandler.HANDLE_PATH + "=/conversations")
public class ConversationApi implements HttpHandler {
    private static final String API_PREFIX = "/conversations";
    private static final Map<String, Boolean> OK_BODY = Collections.singletonMap("ok", true);

    @Reference
    private ConversationStore store;

    @Reference
    private Json json;

    @Reference
    private Logger logger;

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception {
        try {
            dispatch(httpRequest, httpResponse);
        } catch (Throwable t) {
            // 统一兜底：任何未预期异常以 JSON 错误应答，不让连接静默断开
            logger.error("conversation api failed: " + t.getMessage(), t);
            sendError(httpResponse, 500, "INTERNAL");
        }
    }

    private void dispatch(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception {
        String rest = restPath(httpRequest);
        if (rest == null || (!rest.isEmpty() && !rest.startsWith("/"))) {
            sendError(httpResponse, 400, "BAD_REQUEST");
            return;
        }

        String userId = resolveUsername(httpRequest);

        // GET /conversations → 会话列表
        if (rest.isEmpty() || rest.equals("/")) {
            if (!"GET".equals(httpRequest.getMethod())) {
                sendError(httpResponse, 405, "METHOD_NOT_ALLOWED");
                return;
            }
            listConversations(httpResponse, userId);
            return;
        }

        String[] segments = rest.substring(1).split("/", -1);
        String conversationId = segments[0];
        if (conversationId.isEmpty()) {
            sendError(httpResponse, 404, "NOT_FOUND");
            return;
        }

        // PATCH/DELETE /conversations/{id}
        if (segments.length == 1) {
            if ("PATCH".equals(httpRequest.getMethod())) {
                renameConversation(httpRequest, httpResponse, userId, conversationId);
            } else if ("DELETE".equals(httpRequest.getMethod())) {
                deleteConversation(httpResponse, userId, conversationId);
            } else {
                sendError(httpResponse, 405, "METHOD_NOT_ALLOWED");
            }
            return;
        }

        // GET /conversations/{id}/messages
        if (segments.length == 2 && "messages".equals(segments[1])) {
            if (!"GET".equals(httpRequest.getMethod())) {
                sendError(httpResponse, 405, "METHOD_NOT_ALLOWED");
                return;
            }
            listMessages(httpResponse, userId, conversationId);
            return;
        }

        sendError(httpResponse, 404, "NOT_FOUND");
    }

    private void listConversations(HttpResponse httpResponse, String userId) throws Exception {
//        List<ConversationSummary> conversations = index.list(userId);
//        Map<String, Object> body = new HashMap<>();
//        body.put("conversations", conversations);
//        sendJson(httpResponse, 200, body);
    }

    private void listMessages(HttpResponse httpResponse, String userId, String conversationId) throws Exception {
        // 会话不存在或归属不符时接口契约返回空列表，前端据此以空会话呈现
//        List<HistoryMessage> messages = index.contains(userId, conversationId)
//                ? store.listMessages(conversationId) : Collections.emptyList();
//        Map<String, Object> body = new HashMap<>();
//        body.put("conversationId", conversationId);
//        body.put("messages", messages);
//        sendJson(httpResponse, 200, body);
    }

    private void renameConversation(HttpRequest httpRequest, HttpResponse httpResponse, String userId,
                                    String conversationId) throws Exception {
//        Map<String, Object> params = parseBody(httpRequest);
//        if (params == null) {
//            sendError(httpResponse, 400, "BAD_REQUEST");
//            return;
//        }
//        Object title = params.get("title");
//        // 空串/非字符串统一归一为 null（未命名），与索引层"null 表示未命名"的语义对齐
//        String titleStr = title instanceof String && !((String) title).trim().isEmpty()
//                ? ((String) title).trim() : null;
//        if (!index.rename(userId, conversationId, titleStr)) {
//            sendError(httpResponse, 404, "NOT_FOUND");
//            return;
//        }
//        sendJson(httpResponse, 200, OK_BODY);
    }

    private void deleteConversation(HttpResponse httpResponse, String userId, String conversationId) throws Exception {
        // 先删索引（未命中即 404，且不删除数据），命中后联动删除会话记录
//        if (!index.remove(userId, conversationId)) {
//            sendError(httpResponse, 404, "NOT_FOUND");
//            return;
//        }
//        store.delete(conversationId);
//        sendJson(httpResponse, 200, OK_BODY);
    }

    private Map<String, Object> parseBody(HttpRequest httpRequest) {
        byte[] body = httpRequest.getBody();
        if (body == null || body.length == 0) return null;
        try {
            return json.fromJson(new String(body, StandardCharsets.UTF_8), HashMap.class);
        } catch (Exception e) {
            // 仅记录异常摘要，不输出请求体原文（可能含会话标题等用户内容）
            logger.warn("failed to parse conversation api request body: " + e.getMessage());
            return null;
        }
    }

    /**
     * 注册前缀（/ai/conversations）之后的剩余路径；前缀定位失败（防御）返回 null
     */
    private String restPath(HttpRequest httpRequest) {
        String path = httpRequest.getPath();
        if (path == null) return null;
        int idx = path.indexOf(API_PREFIX);
        return idx < 0 ? null : path.substring(idx + API_PREFIX.length());
    }

    /**
     * 与 AiChat 对话链路同源：userId 由鉴权层从 token 解析，显式关闭鉴权时退化为匿名
     */
    private String resolveUsername(HttpRequest httpRequest) {
        Object principal = httpRequest.getAttribute(AuthResult.AUTH_PRINCIPAL_ATTRIBUTE);
        String username = principal instanceof String ? (String) principal : null;
        return username != null && !username.isEmpty() ? username : "anonymous";
    }

    private void sendJson(HttpResponse httpResponse, int status, Object body) throws Exception {
        httpResponse.status(status).contentTypeJsonUtf8().sendFinish(json.toJson(body));
    }

    private void sendError(HttpResponse httpResponse, int status, String code) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        sendJson(httpResponse, status, body);
    }
}
