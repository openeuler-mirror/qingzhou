package qingzhou.ai.memory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import qingzhou.llm.ChatModel;
import qingzhou.llm.ChatModelFactory;
import qingzhou.logger.Logger;

/**
 * 压缩器：单线程串行，把超出稳态留存条数的旧消息折叠成摘要。
 * LLM 调用在锁外发起，不阻塞会话读写；按字符预算分批，使单次输入有界、回退损失限于一批。
 * 增量压缩的边界记在 summaryUpTo 上，压缩任务不跨进程存活
 */
final class ConversationCompactor {
    private static final String SUMMARY_PROMPT =
            "你是对话记忆压缩器。请把【已有摘要】与【新增对话】合并成一份简洁的对话摘要，保留后续继续对话所必需的信息，"
                    + "按以下小标题组织要点，无内容的标题可省略：\n"
                    + "- 用户偏好与习惯\n- 关键事实与结论\n- 未完成的待办\n- 涉及的资源（应用/模块/实例/数据标识）\n"
                    + "只输出摘要正文，不要寒暄、不要复述原文、不要输出任何解释。";

    private final ConversationStore store;
    private final MemoryLimits limits;
    private final Logger logger;
    private final Supplier<ChatModelFactory> modelFactory;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "qz-ai-compactor");
        thread.setDaemon(true);
        return thread;
    });

    ConversationCompactor(ConversationStore store, MemoryLimits limits, Logger logger,
                          Supplier<ChatModelFactory> modelFactory) {
        this.store = store;
        this.limits = limits;
        this.logger = logger;
        this.modelFactory = modelFactory;
    }

    void submit(String conversationId) {
        executor.execute(() -> compact(conversationId));
    }

    void shutdown() {
        executor.shutdownNow();
    }

    /**
     * 先锁内取一次快照确定本次任务范围（有界），再按字符预算分批：每批锁外调 LLM、锁内按 id 回写
     */
    void compact(String conversationId) {
        List<StoredMessage> queue;
        String summarySoFar;
        synchronized (store) {
            ConversationRecord record = store.load(conversationId);
            if (record == null || record.archived || record.messages == null) return;
            if (record.messages.size() <= limits.keepMessages) return;
            queue = afterSummaryUpTo(
                    new ArrayList<>(record.messages.subList(0, record.messages.size() - limits.keepMessages)),
                    record.summaryUpTo);
            if (queue.isEmpty()) return;
            summarySoFar = record.summary;
        }

        int from = 0;
        while (from < queue.size()) {
            int to = batchEnd(queue, from);
            List<StoredMessage> batch = new ArrayList<>(queue.subList(from, to));
            String summary = summarize(summarySoFar, batch);
            if (!merge(conversationId, batch, summary)) return;
            summarySoFar = summary;
            from = to;
        }
    }

    /**
     * 批次边界：按字符预算累加，至少 1 条以保证循环前进
     */
    private int batchEnd(List<StoredMessage> queue, int from) {
        int chars = 0;
        int to = from;
        while (to < queue.size()) {
            int next = chars + MemoryTexts.length(queue.get(to).content);
            if (to > from && next > limits.compactionInputMaxChars) break;
            chars = next;
            to++;
        }
        return to;
    }

    /**
     * 按 id 定向移除而非整体替换：压缩期间的并发写入不会被覆盖
     */
    private boolean merge(String conversationId, List<StoredMessage> batch, String summary) {
        synchronized (store) {
            ConversationRecord record = store.load(conversationId);
            if (record == null || record.archived || record.messages == null) return false;
            Set<String> summarized = new HashSet<>();
            for (StoredMessage message : batch) summarized.add(message.id);
            record.messages.removeIf(message -> message.id != null && summarized.contains(message.id));
            record.summary = summary;
            record.summaryUpTo = batch.get(batch.size() - 1).id;
            record.updatedAt = System.currentTimeMillis();
            return store.save(conversationId, record); // 落库失败即停止后续批次，避免边界与实际内容脱节
        }
    }

    /**
     * 优先 LLM；未配置模型或调用失败时回退拼接式，仅告警一次，不影响会话继续使用
     */
    private String summarize(String previousSummary, List<StoredMessage> batch) {
        String spliced = MemoryTexts.spliceSummary(previousSummary, batch, limits.summaryMaxChars);
        ChatModelFactory factory = modelFactory.get();
        if (factory == null) return spliced;

        try {
            StringBuilder prompt = new StringBuilder(SUMMARY_PROMPT);
            if (previousSummary != null && !previousSummary.isEmpty()) {
                prompt.append("\n\n[已有摘要]\n").append(previousSummary);
            }
            prompt.append("\n\n[新增对话]\n");
            for (StoredMessage message : batch) {
                prompt.append("user".equals(message.role) ? "用户：" : "助手：").append(message.content).append('\n');
            }
            ChatModel model = factory.newChatModelBuilder()
                    // 异常、超时、限流一律直接回退拼接摘要，不重试：压缩是后台任务，不该长期占用资源
                    .connectTimeout(15_000)
                    .readTimeout(60_000)
                    .maxRetries(0)
                    .build();
            String answer = model.chat(prompt.toString());
            if (answer != null && !answer.trim().isEmpty()) {
                return MemoryTexts.truncate(answer.trim(), limits.summaryMaxChars);
            }
        } catch (Throwable t) {
            logger.warn("failed to summarize conversation, fallback to concatenation: " + t.getMessage());
        }
        return spliced;
    }

    /**
     * 增量压缩边界：只摘要 summaryUpTo 之后的消息；边界消息已被删除时退化为全量
     */
    private static List<StoredMessage> afterSummaryUpTo(List<StoredMessage> messages, String summaryUpTo) {
        if (summaryUpTo == null) return messages;
        for (int i = 0; i < messages.size(); i++) {
            if (summaryUpTo.equals(messages.get(i).id)) {
                return new ArrayList<>(messages.subList(i + 1, messages.size()));
            }
        }
        return messages;
    }
}
