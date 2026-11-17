package qingzhou.ai.memory.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import qingzhou.llm.ChatModel;
import qingzhou.llm.ChatModelFactory;
import qingzhou.logger.Logger;

/**
 * 压缩器：单线程串行，把超出稳态留存条数的旧消息折叠成摘要。
 * LLM 调用在锁外发起，不阻塞会话读写；按字符预算分批，使单次输入有界、回退损失限于一批。
 * 压缩任务不跨进程存活，未完成部分由下次写入重新触发
 */
final class ConversationCompactor {
    private static final int QUEUE_CAPACITY = 256;
    private static final String SUMMARY_PROMPT =
            "你是对话记忆压缩器。请把【已有摘要】与【新增对话】合并成一份简洁的对话摘要，保留后续继续对话所必需的信息，"
                    + "按以下小标题组织要点，无内容的标题可省略：\n"
                    + "- 用户偏好与习惯\n- 关键事实与结论\n- 未完成的待办\n- 涉及的资源（应用/模块/实例/数据标识）\n"
                    + "只输出摘要正文，不要寒暄、不要复述原文、不要输出任何解释。";

    private final ConversationRepository repository;
    private final MemoryLimits limits;
    private final Logger logger;
    private final Supplier<ChatModelFactory> modelFactory;
    private final Set<String> pending = ConcurrentHashMap.newKeySet();
    private final AtomicLong dropped = new AtomicLong();
    private final ExecutorService executor;

    ConversationCompactor(ConversationRepository repository, MemoryLimits limits, Logger logger,
                          Supplier<ChatModelFactory> modelFactory) {
        this.repository = repository;
        this.limits = limits;
        this.logger = logger;
        this.modelFactory = modelFactory;
        // 有界队列 + 同会话在途去重：压缩幂等，队列满时丢弃任务安全（后续写入会重新提交）
        this.executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                task -> {
                    Thread thread = new Thread(task, "qz-ai-compactor");
                    thread.setDaemon(true);
                    return thread;
                });
    }

    void submit(String conversationId) {
        if (!pending.add(conversationId)) return;
        try {
            executor.execute(() -> compactPending(conversationId));
        } catch (RejectedExecutionException e) {
            pending.remove(conversationId);
            long count = dropped.incrementAndGet();
            if (count == 1 || count % 100 == 0) {
                logger.warn("compaction queue is full, dropped tasks: " + count);
            }
        }
    }

    void shutdown() {
        executor.shutdownNow();
    }

    private void compactPending(String conversationId) {
        try {
            compact(conversationId);
        } catch (RuntimeException e) {
            logger.warn("failed to compact conversation " + conversationId + ": " + e.getMessage());
        } finally {
            pending.remove(conversationId);
        }
    }

    /**
     * 先锁内取一次快照确定本次任务范围（有界），再按字符预算分批：每批锁外调 LLM、锁内按 id 回写
     */
    private void compact(String conversationId) {
        List<StoredMessage> queue;
        String summarySoFar;
        synchronized (repository) {
            ConversationRecord record = repository.load(conversationId);
            if (record == null || record.archived || record.messages == null) return;
            if (record.messages.size() <= limits.keepMessages) return;
            queue = new ArrayList<>(record.messages.subList(0, record.messages.size() - limits.keepMessages));
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
     * 按 id 定向移除而非整体替换：压缩期间的并发写入不会被覆盖。
     * 不回写 updatedAt：活动时间只由 append 维护，避免积压的压缩任务打乱列表顺序与淘汰次序
     */
    private boolean merge(String conversationId, List<StoredMessage> batch, String summary) {
        synchronized (repository) {
            ConversationRecord record = repository.load(conversationId);
            if (record == null || record.archived || record.messages == null) return false;
            Set<String> summarized = new HashSet<>();
            for (StoredMessage message : batch) summarized.add(message.id);
            record.messages.removeIf(message -> message.id != null && summarized.contains(message.id));
            record.summary = summary;
            return repository.tryPersist(conversationId, record); // 落库失败即停止后续批次，避免摘要与实际内容脱节
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
                prompt.append(MemoryTexts.rolePrefix(message.role)).append(message.content).append('\n');
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
}
