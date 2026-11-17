package qingzhou.ai.chat.impl;

import java.nio.charset.StandardCharsets;
import java.util.*;

import qingzhou.http.server.HttpRequest;
import qingzhou.json.Json;
import qingzhou.llm.Attachment;
import qingzhou.llm.ChatModelFactory;
import qingzhou.logger.Logger;

public class ChatRequest {
    private static final int MAX_QUESTION_CHARS = 10_000;
    // 会话标识由前端生成；只限长度：标识直接成为存储 key，超长值在内存后端会放大堆占用与列表响应
    private static final int MAX_CONVERSATION_ID_CHARS = 64;

    final String question;
    final List<String> refDocs;
    final Attachment[] images;
    final String conversationId;
    final String userId;
    final String[] userRoles;

    ChatRequest(String question, List<String> refDocs, Attachment[] images,
                String conversationId, String userId, String[] userRoles) {
        this.question = question;
        this.refDocs = refDocs;
        this.images = images;
        this.conversationId = conversationId;
        this.userId = userId;
        this.userRoles = userRoles;
    }

    // 解析并校验请求参数；非法时写入 400 并返回 null
    static ChatRequest parseRequest(HttpRequest httpRequest,
                                    ChatModelFactory chatModelFactory, Json json, Logger logger) {
        Map<String, Object> params = parseJsonParams(httpRequest, json, logger);
        if (params == null) return null;

        // 单次提问字符上限：正常业务提问远低于此值，超出即视为滥用
        String question = ((String) params.get("question")).trim();
        if (question.length() > MAX_QUESTION_CHARS) {
            return null;
        }

        // app 为可选参数：未传或空串表示"不限应用范围"；类型非法（非字符串）回 400，避免 try 块外强转 CCE 变 500
        Object appValue = params.get("app");
        String app;
        if (appValue == null) {
            app = "";
        } else if (appValue instanceof String) {
            app = (String) appValue;
        } else {
            return null;
        }
        if (!app.isEmpty()) {
            question = ("在“" + app + "”应用范围内，回复：" + question);
        }

        // 服务端强制 ChatConfig 对前端宣称的限额：附件条数与大小不设防会被用于放大内存与 token 开销
        Map<String, List<String>> attachments = findAttachments(params);
        for (List<String> items : attachments.values()) {
            if (items.size() > ChatConfig.MAX_ATTACHMENT_FILES) {
                return null;
            }
            for (String content : items) {
                if (content.length() > ChatConfig.MAX_ATTACHMENT_CHARS) {
                    return null;
                }
            }
        }

        String conversationId = parseConversationId(params);
        if (conversationId == null) { // 缺失或超长：消息无处落库，直接拒绝
            return null;
        }

        // userId 由鉴权层从 token 解析，不接受前端传参
        List<String> refDocs = attachments.get("document");
        Attachment[] images = attachments.getOrDefault("image", Collections.emptyList()).stream()
                .map(ChatRequest::parseImage)
                .map(image -> chatModelFactory.newImageAttachment(image.base64, image.mimeType))
                .toArray(Attachment[]::new);
        return new ChatRequest(question, refDocs, images, conversationId, httpRequest.getPrincipal(), httpRequest.getRoles());
    }

    private static Map<String, Object> parseJsonParams(HttpRequest httpRequest, Json json, Logger logger) {
        byte[] body = httpRequest.getBody();
        if (body != null && body.length > 0) {
            String str = new String(body, StandardCharsets.UTF_8);
            try {
                Map<String, Object> params = json.fromJson(str, HashMap.class);
                String question = (String) params.get("question");
                if (question != null && !question.trim().isEmpty()) {
                    return params;
                }
            } catch (Exception e) {
                // 仅记录请求体长度与异常，不输出原文：请求体可能含问题原文、附件文本或 base64 图片等敏感内容
                logger.error("failed to convert request body to JSON, body length=" + str.length(), e);
            }
        }
        return null;
    }

    private static Map<String, List<String>> findAttachments(Map<String, Object> params) {
        Map<String, List<String>> found = new HashMap<>();
        Object attachments = params.get("attachments");
        if (attachments instanceof List) {
            for (Object item : (List<?>) attachments) {
                if (!(item instanceof Map)) continue;
                Map<?, ?> map = (Map<?, ?>) item;
                String type = map.get("type") == null ? null : String.valueOf(map.get("type"));
                String content = map.get("content") == null ? null : String.valueOf(map.get("content"));
                if (content == null || content.isEmpty()) continue;
                found.computeIfAbsent(type, k -> new ArrayList<>()).add(content);
            }
        }
        return found;
    }

    /**
     * 会话标识由前端生成，经 RUN_STARTED 回显为权威值；缺失或超长返回 null（调用方按 400 处理）
     */
    private static String parseConversationId(Map<String, Object> params) {
        Object value = params.get("conversationId");
        if (!(value instanceof String)) return null;
        String conversationId = ((String) value).trim();
        return conversationId.isEmpty() || conversationId.length() > MAX_CONVERSATION_ID_CHARS ? null : conversationId;
    }

    // 兼容完整 data URI 与纯 base64 两种前端格式
    private static ParsedImage parseImage(String content) {
        if (content.startsWith("data:")) {
            int comma = content.indexOf(',');
            // 缺逗号/分号分隔符时按纯 base64 处理，避免 substring 越界把恶意请求变成 500
            if (comma < 0) return new ParsedImage(content, null);
            String meta = content.substring("data:".length(), comma);
            int sep = meta.indexOf(';');
            String mime = sep < 0 ? meta : meta.substring(0, sep);
            return new ParsedImage(content.substring(comma + 1), mime);
        }
        return new ParsedImage(content, null);
    }

    private static class ParsedImage {
        final String base64;
        final String mimeType;

        ParsedImage(String base64, String mimeType) {
            this.base64 = base64;
            this.mimeType = mimeType;
        }
    }
}
