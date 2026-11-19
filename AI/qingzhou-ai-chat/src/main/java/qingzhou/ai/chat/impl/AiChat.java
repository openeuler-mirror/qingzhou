package qingzhou.ai.chat.impl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.*;
import qingzhou.ai.SkillService;
import qingzhou.ai.ToolCallInterceptionService;
import qingzhou.ai.chat.ConversationStore;
import qingzhou.common.Configs;
import qingzhou.http.server.*;
import qingzhou.json.Json;
import qingzhou.llm.*;
import qingzhou.logger.Logger;
import qingzhou.monitor.api.Metric;
import qingzhou.monitor.api.MetricProvider;

@Component(configurationPid = "qingzhou-ai", configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {
                HttpHandler.HANDLE_PATH + "=/ai/chat/stream",
                HttpHandler.HANDLE_PATH_ABSOLUTE + "=true",
                HttpHandler.HANDLE_METHODS + "=POST",
                MetricProvider.PROVIDER_ID + "=ai_chat"})
public class AiChat implements HttpHandler, MetricProvider {
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

    // 单篇技能说明书/参考文档注入系统提示的字符上限
    private int maxDocChars;

    // 上下文预算：总预算与输出预留归对话侧，会话记忆只负责在剩余预算内取历史
    private int historyTotalBudget;
    private int historyOutputReserve;

    @Reference
    private Logger logger;
    @Reference
    private Json json;
    @Reference
    private ChatModelFactory chatModelFactory;
    @Reference
    private ConversationStore conversationStore;
    @Reference
    private ToolCallInterceptionService interceptionService;
    @Reference
    private LlmConverter llmConverter;

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
        SseWatchdog.getExecutorService().shutdownNow();
    }

    @Activate
    public void init(Map<String, String> config) {
        Configs configs = new Configs(config);
        maxDocChars = Math.toIntExact(configs.between("chat_max_doc_chars", 1, 1_000_000, 50_000));// 主流 API 梯队：普遍在 100 万 token（1M）左右
        historyTotalBudget = configs.atLeast("history_total_budget", 1, 20000);
        historyOutputReserve = configs.atLeast("history_output_reserve", 0, 4000);
    }

    /**
     * 会话侧健康度：活跃流反映并发压力，超时与客户端断连反映交互质量。
     */
    @Override
    public Set<Metric> collectMetrics() {
        Set<Metric> metrics = new HashSet<>();
        metrics.add(Metric.of("streams_active", ChatMetrics.streamsActive(), Metric.Type.GAUGE,
                "Chat sessions currently streaming to a client"));
        metrics.add(Metric.of("timeouts_total", ChatMetrics.hardTimeouts(), Metric.Type.COUNTER,
                Collections.singletonMap("kind", "hard"), "Chat sessions terminated by the watchdog"));
        metrics.add(Metric.of("timeouts_total", ChatMetrics.firstContentTimeouts(), Metric.Type.COUNTER,
                Collections.singletonMap("kind", "first_content"), "Chat sessions terminated by the watchdog"));
        metrics.add(Metric.of("client_disconnects_total", ChatMetrics.clientDisconnects(), Metric.Type.COUNTER,
                "Chat sessions aborted because the client went away"));
        return metrics;
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {
        ChatRequest request = ChatRequest.parseRequest(httpRequest, chatModelFactory, json, logger);
        if (request == null) {
            httpResponse.status(400);
            return;
        }

        // 模型回调在后台线程写响应，须在返回前声明异步，框架不再自动收尾
        httpResponse.async();
        httpResponse.contentType("text/event-stream; charset=utf-8")
                .header("connection", "keep-alive")
                .header("cache-control", "no-cache")
                .header("x-accel-buffering", "no"); // 告知反代（如 nginx）不要缓冲 SSE，否则事件会攒到连接结束才一次性到达

        SseChatListener sseListener = null;
        try {
            Collection<Skill> skills = llmConverter.convertSkills(llmSkills, logger); // 只转换一次，开销统计与实际注入同源

            // 历史快照须在提问落库前取定：chat() 会把它拼在本轮提问之前，落库后再取会重复注入本轮问题
            List<ChatMemory.Message> memory = conversationStore.getMessageList(request.userId, request.conversationId,
                    historyBudget(skills, request.refDocs, request.question.length()));

            ChatModelFactory.ChatModelBuilder builder = chatModelFactory.newChatModelBuilder()
                    .systemPrompt(SYSTEM_PROMPT)
                    .docs(request.refDocs)
                    .skills(skills)
                    .maxToolResultChars(20480)
                    .enableThinking(true)
                    .interceptor((toolName, argsMap) -> interceptionService.intercept(toolName, argsMap, request.userId, request.userRoles))
                    .maxDocChars(maxDocChars)
                    .chatMemory(() -> memory);
            ChatModel chatModel = builder.build();

            sseListener = startSseChatListener(request, httpResponse);
            storeUserMessage(request, sseListener); // 先落库再启动模型：落库失败不消耗一次模型调用
            chatModel.chat(request.question, sseListener, request.images);
        } catch (Throwable t) {
            // 受理后的任何前置异常（模型未配置、技能配置解析失败等）都必须以事件告知客户端，
            // 否则连接被静默断开，前端会一直停留在“AI 正在思考...”
            String msg = t.getMessage() != null && !t.getMessage().isEmpty() ? t.getMessage() : t.toString();
            logger.error("ai chat request failed: " + msg, t);
            if (sseListener != null) {
                sseListener.onError(msg);
            } else {
                httpResponse.finish(); // 监听器未建立：直接终止响应，避免连接悬挂
            }
        }
    }

    private void storeUserMessage(ChatRequest request, SseChatListener sseListener) {
        String userMessageId = UUID.randomUUID().toString();
        conversationStore.storeUserMessage(request.userId, request.conversationId, request.question, userMessageId);
        sseListener.sendUserMessage(userMessageId); // 消息 id 随 USER_MESSAGE 下发，供前端问答成对删除
    }

    private SseChatListener startSseChatListener(ChatRequest request, HttpResponse httpResponse) {
        SseChatListener sseListener = new SseChatListener(httpResponse, logger, json);
        String assistantMessageId = UUID.randomUUID().toString(); // AI 回复落库 id 预生成：随 RUN_STARTED 下发，onComplete 落库时对齐同一 id
        sseListener.setStarted(request.conversationId, assistantMessageId, new SseChatListener.MessageAggregator() {
            @Override
            void aggregatedMessage(String content) {
                // 流正常结束后的动作（AiChat 落库回答全文：chat() 异步，全文须等 onComplete 才完整）
                conversationStore.storeAssistantMessage(request.userId, request.conversationId, content, assistantMessageId);
            }
        });
        return sseListener;
    }

    /**
     * 可注入历史的字符预算：总预算扣除系统提示、问题原文、技能说明书、附件文档与输出预留。
     * 说明文档按注入时的截断上限计入（技能未激活也计入，属保守上界）；不足时可为负，由会话记忆按保底处理
     */
    private int historyBudget(Collection<Skill> skills, List<String> docs, int questionChars) {
        int budget = historyTotalBudget - historyOutputReserve - SYSTEM_PROMPT.length() - questionChars;
        for (Skill skill : skills) {
            budget -= Math.min(length(skill.description()), maxDocChars);
        }
        if (docs != null) {
            for (String doc : docs) {
                budget -= Math.min(length(doc), maxDocChars);
            }
        }
        return budget;
    }

    private static int length(String text) {
        return text == null ? 0 : text.length();
    }
}
