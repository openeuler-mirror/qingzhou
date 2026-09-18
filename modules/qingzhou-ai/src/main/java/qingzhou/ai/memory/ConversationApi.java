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

@Component(property = HttpHandler.HANDLE_PATH + "=" + ConversationApi.API_PREFIX)
public class ConversationApi implements HttpHandler {
    public static final String API_PREFIX = "/conversations";
    private static final Map<String, Boolean> OK_BODY = Collections.singletonMap("ok", true);

    /**
     * userId 由鉴权层从 token 解析；未开鉴权时为匿名
     */
    public static String resolveUserId(HttpRequest httpRequest) {
        Object principal = httpRequest.getAttribute(AuthResult.AUTH_PRINCIPAL_ATTRIBUTE);
        String s = principal instanceof String ? (String) principal : null;
        return s != null && !s.isEmpty() ? s : "anonymous";
    }

    @Reference
    private ConversationStore store;

    @Reference
    private Json json;

    @Reference
    private Logger logger;

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception {
        try {
            String rest = null;
            String path = httpRequest.getPath();
            if (path != null) {
                int idx = path.indexOf(API_PREFIX);
                rest = idx < 0 ? null : path.substring(idx + API_PREFIX.length());
            }

            if (rest == null || (!rest.isEmpty() && !rest.startsWith("/"))) {
                sendError(httpResponse, 400, "BAD_REQUEST");
                return;
            }

            dispatch(httpRequest, httpResponse, rest);
        } catch (Throwable t) {
            // 统一兜底：任何未预期异常以 JSON 错误应答，不让连接静默断开
            logger.error("conversation api failed: " + t.getMessage(), t);
            sendError(httpResponse, 500, "INTERNAL");
        }
    }

    private void dispatch(HttpRequest httpRequest, HttpResponse httpResponse, String rest) throws Exception {
        String userId = resolveUserId(httpRequest);

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

        // DELETE /conversations/{id}/messages/{messageId} → 删除单条消息（如单次回答）
        if (segments.length == 3 && "messages".equals(segments[1])) {
            if (!"DELETE".equals(httpRequest.getMethod())) {
                sendError(httpResponse, 405, "METHOD_NOT_ALLOWED");
                return;
            }
            deleteMessage(httpResponse, userId, conversationId, segments[2]);
            return;
        }

        sendError(httpResponse, 404, "NOT_FOUND");
    }

    private void listConversations(HttpResponse httpResponse, String userId) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("conversations", store.listConversations(userId));
        sendJson(httpResponse, 200, body);
    }

    private void listMessages(HttpResponse httpResponse, String userId, String conversationId) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("conversationId", conversationId);
        body.put("messages", store.listMessages(userId, conversationId));
        sendJson(httpResponse, 200, body);
    }

    private void renameConversation(HttpRequest httpRequest, HttpResponse httpResponse, String userId,
                                    String conversationId) throws Exception {
        Map<String, Object> params = null;
        byte[] body = httpRequest.getBody();
        if (body != null && body.length > 0) {
            try {
                params = json.fromJson(new String(body, StandardCharsets.UTF_8), HashMap.class);
            } catch (Exception e) {
                // 仅记录异常摘要，不输出请求体原文（可能含会话标题等用户内容）
                logger.warn("failed to parse conversation api request body: " + e.getMessage());
            }
        }
        if (params == null) {
            sendError(httpResponse, 400, "BAD_REQUEST");
            return;
        }

        Object title = params.get("title");
        // 空串/非字符串归一为 null（未命名）
        String titleStr = title instanceof String && !((String) title).trim().isEmpty()
                ? ((String) title).trim() : null;
        if (!store.rename(userId, conversationId, titleStr)) {
            sendError(httpResponse, 404, "NOT_FOUND");
            return;
        }
        sendJson(httpResponse, 200, OK_BODY);
    }

    private void deleteConversation(HttpResponse httpResponse, String userId, String conversationId) throws Exception {
        if (!store.remove(userId, conversationId)) {
            sendError(httpResponse, 404, "NOT_FOUND");
            return;
        }
        sendJson(httpResponse, 200, OK_BODY);
    }

    private void deleteMessage(HttpResponse httpResponse, String userId, String conversationId,
                               String messageId) throws Exception {
        if (!store.removeMessage(userId, conversationId, messageId)) {
            sendError(httpResponse, 404, "NOT_FOUND");
            return;
        }
        sendJson(httpResponse, 200, OK_BODY);
    }

    private void sendError(HttpResponse httpResponse, int status, String code) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        sendJson(httpResponse, status, body);
    }

    private void sendJson(HttpResponse httpResponse, int status, Object body) throws Exception {
        httpResponse.status(status).contentTypeJsonUtf8().sendFinish(json.toJson(body));
    }
}
