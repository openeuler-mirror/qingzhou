package qingzhou.ai.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.*;
import qingzhou.ai.SkillService;
import qingzhou.ai.ToolInterceptor;
import qingzhou.ai.memory.ConversationApi;
import qingzhou.ai.memory.ConversationStore;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.llm.Attachment;
import qingzhou.llm.ChatModel;
import qingzhou.llm.ChatModelFactory;
import qingzhou.llm.Interceptor;
import qingzhou.logger.Logger;

@Component(property = HttpHandler.HANDLE_PATH + "=/chat/stream")
public class AiChat implements HttpHandler {
    private static final String SYSTEM_PROMPT = "\n" +
            "## 身份定义\n" +
            "你是轻舟平台智能助手，帮助开发者、运维人员和管理员理解和使用轻舟平台\n" +
            "当用户询问关于某系统、某资产或某插件时，统一理解为部署在轻舟平台上的应用\n" +
            "## 回答原则\n" +
            "准确优先：不确定时明确说明，不编造事实\n" +
            "简洁直接：先给结论，再给必要解释\n" +
            "语言跟随：用户用什么语言就用什么语言回答\n" +
            "安全合规：禁止输出机制破解、绕过权限、非法运维等内容\n" +
            "## 内容格式\n" +
            "正常使用 Markdown 回复，善用列表和代码块\n" +
            "如果要生成图表，则输出一个 Markdown 代码块，代码块类型固定为: echarts\n" +
            "代码块内部须是一个完整的 ECharts Option 对象，只输出对象本身，不要生成 const/let/var/import/export/function/HTML/JavaScript/Markdown 包裹\n" +
            "优先使用 bar、line、pie、scatter，根据数据自动选择最合适的图表\n";

    @Reference
    private Logger logger;
    @Reference
    private Json json;
    @Reference
    private ChatModelFactory chatModelFactory;
    @Reference
    private ChatConfig chatConfig;
    @Reference
    private ConversationStore conversationStore;
    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    private final Set<ToolInterceptor> toolInterceptors = ConcurrentHashMap.newKeySet();

    private final Map<SkillService, Map<String, Object>> llmSkills = new HashMap<>();

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    public void bindAiSkill(SkillService skill, Map<String, Object> properties) {
        llmSkills.put(skill, properties);
    }

    // OSGI 框架根据名称规则自动识别调用此方法
    public void unbindAiSkill(SkillService skill) {
        llmSkills.remove(skill);
    }

    @Deactivate
    public void deactivate() {
        SseListener.WATCHDOG_EXECUTOR.shutdownNow();
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) throws IOException {
        Map<String, Object> params = params(httpRequest);
        if (params == null) return;

        String question = ((String) params.get("question")).trim();
        String app = (String) params.get("app");
        if (app != null && !app.isEmpty()) {
            question = ("在“" + app + "”应用范围内，回复：" + question);
        }
        String finalQuestion = question;

        Map<String, List<String>> attachments = findAttachments(params);
        List<String> refDocs = attachments.get("document");
        Attachment[] images = attachments.getOrDefault("image", Collections.emptyList()).stream()
                .map(this::parseImage)
                .map(image -> chatModelFactory.newImageAttachment(image.base64, image.mimeType))
                .toArray(Attachment[]::new);

        // 会话标识，经 RUN_STARTED 下发（前端以其为权威值）
        String conversationId = (String) params.get("conversationId");
        // userId 由鉴权层从 token 解析，不接受前端传参
        String userId = ConversationApi.resolveUserId(httpRequest);
        String[] userRoles = httpRequest.getRoles();

        // 发出响应前的准备
        httpResponse.contentType("text/event-stream; charset=utf-8")
                .header("connection", "keep-alive")
                .header("cache-control", "no-cache")
                .header("x-accel-buffering", "no"); // 告知反代（如 nginx）不要缓冲 SSE，否则事件会攒到连接结束才一次性到达
        SseListener sseListener = new SseListener(httpResponse, logger, json);

        // AI 回复落库 id 预生成：随 RUN_STARTED 下发，onComplete 落库时对齐同一 id
        String assistantMessageId = UUID.randomUUID().toString();
        // chat() 异步返回，回答全文须等流结束才完整，落库挂 onComplete
        sseListener.onCompleteAction(() -> conversationStore.storeAssistantMessage(conversationId, userId, sseListener.allContent.toString(), assistantMessageId));

        try {
            sseListener.setStarted(conversationId, assistantMessageId); // 先告知"已受理"：技能匹配等前置工作可能耗时数秒，不能让客户端误以为请求没发出去
            ChatModelFactory.ChatModelBuilder builder = chatModelFactory.newChatModelBuilder()
                    .systemPrompt(SYSTEM_PROMPT)
                    .docs(refDocs)
                    .skills(LlmConverter.convertSkills(llmSkills, logger))
                    .enableThinking(true)
                    .interceptor(convertInterceptor(userId, userRoles))
                    .chatMemory(() -> conversationStore.getMessageList(userId, conversationId));
            ChatModel chatModel = builder.build();
            chatModel.chat(finalQuestion, sseListener, images);

            // chat() 异步返回，立即落库（在 chatMemory 读取历史之后，避免本轮提问混入上下文重复），
            String storedUserMessageId = conversationStore.storeUserMessage(conversationId, userId, finalQuestion);
            // 保证消息时间戳正确；消息 id 随 USER_MESSAGE 下发，供前端问答成对删除
            sseListener.sendUserMessage(storedUserMessageId);
        } catch (Throwable t) {
            // 受理后的任何前置异常（模型未配置、技能配置解析失败等）都必须以事件告知客户端，
            // 否则连接被静默断开，前端会一直停留在“AI 正在思考...”
            String msg = t.getMessage() != null && !t.getMessage().isEmpty() ? t.getMessage() : t.toString();
            logger.error("ai chat request failed: " + msg, t);
            sseListener.onError(msg);
        }
    }

    private Interceptor convertInterceptor(String userId, String[] userRoles) {
        ToolInterceptor.InterceptorContext context = new ToolInterceptor.InterceptorContext() {
            @Override
            public String getPrincipal() {
                return userId;
            }

            @Override
            public String[] getRoles() {
                return userRoles;
            }
        };
        return (toolName, argsMap) -> {
            for (ToolInterceptor interceptor : toolInterceptors) {
                String result = interceptor.intercept(toolName, argsMap, context);
                if (result != null) return result;
            }
            return null;
        };
    }

    private Map<String, Object> params(HttpRequest httpRequest) {
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

    private Map<String, List<String>> findAttachments(Map<String, Object> params) {
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

    // 兼容完整 data URI 与纯 base64 两种前端格式
    private ParsedImage parseImage(String content) {
        if (content.startsWith("data:")) {
            int comma = content.indexOf(',');
            String meta = content.substring("data:".length(), comma);
            return new ParsedImage(content.substring(comma + 1), meta.substring(0, meta.indexOf(';')));
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
