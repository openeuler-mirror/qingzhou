package qingzhou.ai.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.*;
import qingzhou.ai.SkillService;
import qingzhou.ai.memory.ConversationStore;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.llm.Attachment;
import qingzhou.llm.ChatModel;
import qingzhou.llm.ChatModelFactory;
import qingzhou.llm.Skill;
import qingzhou.logger.Logger;

@Component(property = {HttpHandler.HANDLE_PATH + "=/chat/stream", HttpHandler.HANDLE_METHODS + "=POST"})
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
            "歧义澄清：当多个实例/应用存在同名或相似的模块时，先列出候选并请用户确认要查看哪一个，得到明确指向后再调用业务工具，不要自行猜测或连续查询多个同名模块\n" +
            "## 内容格式\n" +
            "正常使用 Markdown 回复，善用列表和代码块\n" +
            "如果要生成图表，则输出一个 Markdown 代码块，代码块类型固定为: echarts\n" +
            "代码块内部须是一个完整的 ECharts Option 对象，只输出对象本身，不要生成 const/let/var/import/export/function/HTML/JavaScript/Markdown 包裹\n" +
            "优先使用 bar、line、pie、scatter，根据数据自动选择最合适的图表\n";

    private static final int MAX_QUESTION_CHARS = 10_000;

    @Reference
    private Logger logger;
    @Reference
    private Json json;
    @Reference
    private ChatModelFactory chatModelFactory;
    @Reference
    private ConversationStore conversationStore;
    @Reference
    private InterceptToolInvocation interceptToolInvocation;

    private final Map<SkillService, Map<String, Object>> llmSkills = new ConcurrentHashMap<>();

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
        // 刻意停用即关停共享看门狗：在途会话会失去心跳与首字超时（悬挂到客户端超时），
        // 但不关停会让空闲线程经 Worker→Executor→线程工厂持有旧 bundle 的类加载器，导致 bundle 更新后泄漏
        SseListener.watchdog().shutdownNow();
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) throws IOException {
        Map<String, Object> params = params(httpRequest);
        if (params == null) {
            httpResponse.status(400);
            return;
        }
        ChatRequest request = parseRequest(params, httpRequest, httpResponse);
        if (request == null) return; // 参数非法：400 已由 parseRequest 写入
        startChat(request, httpResponse);
    }

    // 解析并校验后的会话请求：构造完成后各字段只读
    private static final class ChatRequest {
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
    }

    // 解析并校验请求参数；非法时写入 400 并返回 null
    private ChatRequest parseRequest(Map<String, Object> params, HttpRequest httpRequest, HttpResponse httpResponse) {
        // 单次提问字符上限：正常业务提问远低于此值，超出即视为滥用
        String question = ((String) params.get("question")).trim();
        if (question.length() > MAX_QUESTION_CHARS) {
            httpResponse.status(400);
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
            httpResponse.status(400);
            return null;
        }
        if (!app.isEmpty()) {
            question = ("在“" + app + "”应用范围内，回复：" + question);
        }

        // 服务端强制 ChatConfig 对前端宣称的限额：附件条数与大小不设防会被用于放大内存与 token 开销
        Map<String, List<String>> attachments = findAttachments(params);
        for (List<String> items : attachments.values()) {
            if (items.size() > ChatConfig.MAX_ATTACHMENT_FILES) {
                httpResponse.status(400);
                return null;
            }
            for (String content : items) {
                if (content.length() > ChatConfig.MAX_ATTACHMENT_CHARS) {
                    httpResponse.status(400);
                    return null;
                }
            }
        }

        // 会话标识，经 RUN_STARTED 下发（前端以其为权威值）：须与落库侧同一规则，否则回答"成功"而消息被静默丢弃
        String conversationId = null;
        Object conversationIdVal = params.get("conversationId");
        if (conversationIdVal instanceof String) {
            conversationId = ((String) conversationIdVal).trim();
        }
        if (!ConversationStore.isValidId(conversationId)) {
            httpResponse.status(400);
            return null;
        }

        // userId 由鉴权层从 token 解析，不接受前端传参
        List<String> refDocs = attachments.get("document");
        Attachment[] images = attachments.getOrDefault("image", Collections.emptyList()).stream()
                .map(this::parseImage)
                .map(image -> chatModelFactory.newImageAttachment(image.base64, image.mimeType))
                .toArray(Attachment[]::new);
        return new ChatRequest(question, refDocs, images, conversationId, httpRequest.getPrincipal(), httpRequest.getRoles());
    }

    // SSE 准备与模型调用；受理后的任何前置异常都以事件告知客户端
    private void startChat(ChatRequest request, HttpResponse httpResponse) {
        // 模型回调在后台线程写响应，须在返回前声明异步，框架不再自动收尾
        httpResponse.async();
        httpResponse.contentType("text/event-stream; charset=utf-8")
                .header("connection", "keep-alive")
                .header("cache-control", "no-cache")
                .header("x-accel-buffering", "no"); // 告知反代（如 nginx）不要缓冲 SSE，否则事件会攒到连接结束才一次性到达
        SseListener sseListener = new SseListener(httpResponse, logger, json);

        // AI 回复落库 id 预生成：随 RUN_STARTED 下发，onComplete 落库时对齐同一 id
        String assistantMessageId = UUID.randomUUID().toString();
        // chat() 异步返回，回答全文须等流结束才完整，落库挂 onComplete
        sseListener.onCompleteAction(() -> conversationStore.storeAssistantMessage(request.conversationId, request.userId, sseListener.allContent.toString(), assistantMessageId));

        try {
            sseListener.setStarted(request.conversationId, assistantMessageId); // 先告知"已受理"：技能匹配等前置工作可能耗时数秒，不能让客户端误以为请求没发出去
            Collection<Skill> skills = LlmConverter.convertSkills(llmSkills, logger); // 只转换一次，开销统计与实际注入同源
            int maxRefChars = 10000; // 单篇技能说明书/参考文档注入系统提示的字符上限
            ChatModelFactory.ChatModelBuilder builder = chatModelFactory.newChatModelBuilder()
                    .systemPrompt(SYSTEM_PROMPT)
                    .docs(request.refDocs)
                    .skills(skills)
                    .enableThinking(true)
                    .interceptor((toolName, argsMap) -> interceptToolInvocation.intercept(toolName, argsMap, request.userId, request.userRoles))
                    .maxDocChars(maxRefChars)
                    .chatMemory(() -> conversationStore.getMessageList(request.userId, request.conversationId,
                            overhead(skills, request.refDocs, request.question.length(), maxRefChars)));
            ChatModel chatModel = builder.build();
            chatModel.chat(request.question, sseListener, request.images);

            // chat() 异步返回，立即落库（在 chatMemory 读取历史之后，避免本轮提问混入上下文重复），
            String storedUserMessageId = conversationStore.storeUserMessage(request.conversationId, request.userId, request.question);
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

    /**
     * 本轮请求的固定开销：系统提示、技能说明书、附件文档、问题原文。
     * 按注入时的截断上限计入，供会话记忆据此裁剪历史注入量；技能未激活时也计入，属保守上界
     */
    private static int overhead(Collection<Skill> skills, List<String> docs, int questionChars, int maxRefChars) {
        int chars = SYSTEM_PROMPT.length() + questionChars;
        for (Skill skill : skills) {
            chars += Math.min(length(skill.description()), maxRefChars);
        }
        if (docs != null) {
            for (String doc : docs) {
                chars += Math.min(length(doc), maxRefChars);
            }
        }
        return chars;
    }

    private static int length(String text) {
        return text == null ? 0 : text.length();
    }
}
