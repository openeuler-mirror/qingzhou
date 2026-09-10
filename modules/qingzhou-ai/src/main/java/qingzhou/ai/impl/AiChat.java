package qingzhou.ai.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import qingzhou.ai.LlmConverter;
import qingzhou.ai.SkillService;
import qingzhou.http.server.AuthResult;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.llm.Attachment;
import qingzhou.llm.ChatMemory;
import qingzhou.llm.ChatModel;
import qingzhou.llm.ChatModelFactory;
import qingzhou.logger.Logger;

@Component(property = HttpHandler.HANDLE_PATH + "=/chat/stream")
public class AiChat implements HttpHandler {
    /** 每用户滑动窗口限流：窗口内请求数达到上限返回 429 + {"code":"RATE_LIMITED"} */
    private static final int RATE_WINDOW_MS = 60_000;
    private static final int RATE_LIMIT_REQUESTS = 20;
    private static final int RATE_WINDOW_CLEANUP_THRESHOLD = 1024;

    /** 记忆模式下注入上下文的最大历史条数，单条长度由记忆实现截断 */
    private static final int HISTORY_MAX_MESSAGES = 20;

    private static final String SYSTEM_PROMPT = "\n" +
            "# 你是一个专业的 Qingzhou（轻舟）平台智能助手，你的职责是帮助开发者、运维人员和管理员理解和使用 Qingzhou 平台。\n" +
            "\n" +
            "## 专业认知 \n" +
            "\n" +
            "- 精通 Qingzhou 的整体架构、核心特性和设计理念。\n" +
            "- 精通 Java 生态、低代码开发、声明式开发、RESTful API 设计、动态渲染。\n" +
            "- 具有丰富的系统运维实践经验。\n" +
            "\n" +
            "## 回答原则 \n" +
            "\n" +
            "1. **准确性优先**：\n" +
            "    - 不编造不存在的功能或接口。\n" +
            "2. **场景化引导**：\n" +
            "    - 当用户询问关于\"某某系统、某某资产、某某插件\"时，可统一理解为某某应用，因为应用是平台上管理的唯一资源，所有问题都可围绕应用进行回答。\n" +
            "    - 当用户询问\"AI管控如何使用\"时，说明自然语言交互通过大模型理解意图并执行管控逻辑。\n" +
            "3. **边界意识**：\n" +
            "    - 如果问题涉及文档未覆盖的具体代码实现细节，如实告知并建议查阅源码或社区。\n" +
            "    - 如果问题与 Qingzhou 无关，礼貌说明你的专业领域并提供力所能及的参考。\n" +
            "    - 如果用户提出平台当前不支持的需求，客观说明现状，可基于架构设计给出可行性分析。\n" +
            "4. **语言风格**：\n" +
            "    - 专业、简洁、结构化，善用列表和代码块，优先给出可操作的步骤和路径。\n" +
            "\n" +
            "## 禁止事项 \n" +
            "\n" +
            "- 不得编造 Qingzhou 未提及的功能、接口或配置项。\n" +
            "- 不得对平台安全性、性能等做出未经验证的承诺性描述。\n" +
            "- 禁止恶意贬低 / 夸大产品能力、跨产品踩一捧一。\n" +
            "- 禁止输出破解、绕过平台安全限制、非法运维相关代码方案。\n" +
            "\n" +
            "## 输出要求 \n" +
            "\n" +
            "正常使用 Markdown 回复，如果用户要求生成图表，请输出一个 Markdown 代码块，代码块类型固定为: echarts。\n" +
            "代码块内部必须是一个 ECharts Option 对象。option的数据必须完整，不要生成：\n" +
            "- const option = \n" +
            "- let option = \n" +
            "- option = \n" +
            "- export \n" +
            "- import \n" +
            "- function \n" +
            "- HTML \n" +
            "- JavaScript \n" +
            "- Markdown \n" +
            "只输出对象本身。优先使用 bar、line、pie、scatter，根据数据自动选择最合适的图表。\n" +
            "\n";

    @Reference
    private ChatModelFactory chatModelFactory;

    @Reference
    private ChatConfig chatConfig;

    @Reference
    private Logger logger;

    @Reference
    private Json json;

    /**
     * 对话记忆：实现由 qingzhou-ai-memory 等独立模块提供，未部署时为 null，退化为单轮无记忆。
     * 动态引用：实现模块热插拔不影响已受理请求（快照值在 handle 内获取）。
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    private volatile ChatMemory memory;

    private final Map<String, ArrayDeque<Long>> rateWindows = new ConcurrentHashMap<>();

    @Deactivate
    public void deactivate() {
        SseListener.WATCHDOG_EXECUTOR.shutdownNow();
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) throws IOException {
        Map<String, Object> params = null;
        String question = null;
        byte[] body = httpRequest.getBody();
        if (body != null && body.length > 0) {
            String str = new String(body, StandardCharsets.UTF_8);
            try {
                params = json.fromJson(str, HashMap.class);
                question = (String) params.get("question");
            } catch (Exception e) {
                // 仅记录请求体长度与异常，不输出原文：请求体可能含问题原文、附件文本或 base64 图片等敏感内容
                logger.error("failed to convert request body to JSON, body length=" + str.length(), e);
            }
        }
        if (params == null || question == null || question.trim().isEmpty()) return;
        question = question.trim();

        // userId 由服务端鉴权层从 token 解析，不接受前端传参；显式关闭鉴权时退化为匿名
        String username = resolveUsername(httpRequest);

        if (!tryAcquire(username)) {
            sendRateLimited(httpResponse);
            return;
        }

        List<String> refDocs = null;
        Attachment[] images = null;
        for (SkillService.AttachmentType attachmentType : SkillService.AttachmentType.values()) {
            List<String> attachments = findAttachments(params, attachmentType);
            switch (attachmentType) {
                case document:
                    refDocs = attachments;
                    break;
                case image:
                    images = attachments.stream().map(s -> chatModelFactory.newImageAttachment(s, null)).toArray(Attachment[]::new);
                    break;
                default:
                    logger.warn("unsupported type: " + attachmentType);
            }
        }

        String app = (String) params.get("app");
        if (app != null && !app.isEmpty()) {
            question = ("在“" + app + "”应用范围内，回复：" + question);
        }

        // 会话标识采信/生成后经 RUN_STARTED 下发（前端以其为权威值）；无记忆实现时仅作透传标识
        ChatMemory memory = this.memory;
        String conversationId = memory != null
                ? memory.resolveConversationId(username, strOrNull(params.get("conversationId")))
                : conversationIdFallback(strOrNull(params.get("conversationId")));
        String messageId = UUID.randomUUID().toString();

        // 发出响应
        httpResponse.contentType("text/event-stream; charset=utf-8")
                .header("connection", "keep-alive")
                .header("cache-control", "no-cache")
                .header("x-accel-buffering", "no"); // 告知反代（如 nginx）不要缓冲 SSE，否则事件会攒到连接结束才一次性到达

        // 先告知"已受理"：技能匹配等前置工作可能耗时数秒，不能让客户端误以为请求没发出去
        SseListener sseListener = new SseListener(httpResponse, logger, json);
        try {
            sseListener.setStarted(conversationId, messageId);
            ChatModelFactory.ChatModelBuilder builder = chatModelFactory.newChatModelBuilder()
                    .systemPrompt(SYSTEM_PROMPT)
                    .docs(refDocs)
                    .skills(LlmConverter.convertAiSkill(chatConfig.llmSkills))
                    .enableThinking(true);
            if (memory != null) {
                builder.memory(memory, username, conversationId, messageId)
                        .maxHistoryMessages(HISTORY_MAX_MESSAGES);
            }
            ChatModel chatModel = builder.build();
            chatModel.chat(question, sseListener, images);
        } catch (Throwable t) {
            // 受理后的任何前置异常（模型未配置、技能配置解析失败等）都必须以事件告知客户端，
            // 否则连接被静默断开，前端会一直停留在“AI 正在思考...”
            String msg = t.getMessage() != null && !t.getMessage().isEmpty() ? t.getMessage() : t.toString();
            logger.error("ai chat request failed: " + msg, t);
            sseListener.onError(msg);
        }
    }

    private String resolveUsername(HttpRequest httpRequest) {
        Object principal = httpRequest.getAttribute(AuthResult.AUTH_PRINCIPAL_USERNAME_ATTRIBUTE);
        String username = principal instanceof String ? (String) principal : null;
        return username != null && !username.isEmpty() ? username : "anonymous";
    }

    private boolean tryAcquire(String userId) {
        // 惰性清理已过期的窗口对象，防止用户量增长导致 map 无界膨胀
        if (rateWindows.size() > RATE_WINDOW_CLEANUP_THRESHOLD) {
            long now = System.currentTimeMillis();
            rateWindows.values().removeIf(window -> {
                synchronized (window) {
                    purgeExpired(window, now);
                    return window.isEmpty();
                }
            });
        }
        long now = System.currentTimeMillis();
        ArrayDeque<Long> window = rateWindows.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (window) {
            purgeExpired(window, now);
            if (window.size() >= RATE_LIMIT_REQUESTS) return false;
            window.addLast(now);
            return true;
        }
    }

    private void purgeExpired(ArrayDeque<Long> window, long now) {
        while (!window.isEmpty() && now - window.peekFirst() > RATE_WINDOW_MS) {
            window.pollFirst();
        }
    }

    private void sendRateLimited(HttpResponse httpResponse) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", "RATE_LIMITED");
        body.put("message", "Too many requests, please try again later");
        try {
            httpResponse.status(429).contentTypeJsonUtf8().sendFinish(json.toJson(body));
        } catch (Exception e) {
            logger.warn("failed to send rate limit response: " + e.getMessage());
        }
    }

    private String strOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /** 无记忆实现时的会话标识兜底：仅做格式校验采信，否则新生成（不做归属校验——无存储可查） */
    private String conversationIdFallback(String requestedId) {
        if (requestedId != null && requestedId.length() >= 8 && requestedId.length() <= 64
                && requestedId.matches("[A-Za-z0-9_-]+")) {
            return requestedId;
        }
        return UUID.randomUUID().toString();
    }

    private List<String> findAttachments(Map<String, Object> params, SkillService.AttachmentType expectedType) {
        List<String> found = new ArrayList<>();
        Object attachments = params.get("attachments");
        if (attachments instanceof List) {
            for (Object item : (List<?>) attachments) {
                if (!(item instanceof Map)) continue;
                Map<?, ?> map = (Map<?, ?>) item;
                String type = map.get("type") == null ? null : String.valueOf(map.get("type"));
                String content = map.get("content") == null ? null : String.valueOf(map.get("content"));
                if (content == null || content.isEmpty()) continue;
                if (Objects.equals(type, expectedType.name())) {
                    found.add(content);
                }
            }
        }
        return found;
    }
}
