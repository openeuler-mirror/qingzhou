package qingzhou.ai.memory.impl;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.*;
import qingzhou.ai.chat.ConversationStore;
import qingzhou.json.Json;
import qingzhou.llm.ChatMemory;
import qingzhou.llm.ChatModelFactory;
import qingzhou.logger.Logger;
import qingzhou.store.Store;
import qingzhou.store.StoreFactory;

/**
 * 会话记忆门面：容量有界 + 长会话可续。
 * 占用有界靠三层降级而非直接丢弃：先压缩（保留摘要）→ 再归档（只留标题与摘要）→ 最后才删除。
 * KV 仍是唯一正本，{@link ConversationCatalog} 是纯派生索引，可随时从 KV 重建
 */
@Component(configurationPid = "qingzhou-ai", configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = {ConversationStoreImpl.class, ConversationStore.class})
public class ConversationStoreImpl implements ConversationStore {
    @Reference
    private Json json;
    @Reference
    private Logger logger;
    @Reference
    private StoreFactory storeFactory;
    // 可选：未加载 llm 时仅丧失摘要能力，会话存取与查询不受影响
    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
    private volatile ChatModelFactory chatModelFactory;

    private MemoryLimits limits;
    private ConversationRepository repository;
    private ConversationWindow window;
    private ConversationAdmin admin;
    private ConversationQuota quota;
    private ConversationCompactor compactor;

    @Activate
    public void init(Map<String, String> config) {
        limits = new MemoryLimits(config);
        boolean persistent = config != null && "file".equalsIgnoreCase(config.get("memory_store"));
        Store kvStore;
        if (persistent) {
            String dir = config.get("memory_store_dir");
            if (dir == null || dir.trim().isEmpty()) dir = "data/ai-conversations";
            kvStore = storeFactory.buildFileStore(Paths.get(config.get("qingzhou.instance"), dir).toFile());
        } else {
            kvStore = storeFactory.buildMemoryStore();
        }
        repository = new ConversationRepository(kvStore, json, logger);
        window = new ConversationWindow(repository, limits, logger);
        admin = new ConversationAdmin(repository, limits);
        quota = new ConversationQuota(repository, limits);
        compactor = new ConversationCompactor(repository, limits, logger, () -> chatModelFactory);

        repository.rebuild();
        synchronized (repository) {
            if (!persistent) {
                // 内存后端：超限即内存已实际占用，必须立即收敛（只归档/淘汰，不调 LLM）
                quota.enforce(null);
            } else if (repository.catalog().overQuota(limits)) {
                // 文件后端：不在启动路径做破坏性动作，交由后续写入逐步收敛
                logger.warn("conversation store already exceeds the configured quota, it will converge on subsequent writes");
            }
        }
    }

    @Deactivate
    public void destroy() {
        compactor.shutdown();
    }

    @Override
    public List<ChatMemory.Message> getMessageList(String userId, String conversationId, int budgetChars) {
        return window.build(userId, conversationId, budgetChars);
    }

    @Override
    public void storeUserMessage(String userId, String conversationId, String content, String messageId) {
        append(userId, conversationId, message("user", content, messageId));
    }

    @Override
    public void storeAssistantMessage(String userId, String conversationId, String content, String messageId) {
        append(userId, conversationId, message("assistant", content, messageId));
    }

    // 管理与监控入口：ConversationApi、MemoryMetricProvider 注入本门面
    List<ConversationSummary> listConversations(String userId) {
        return admin.list(userId);
    }

    List<HistoryMessage> listMessages(String userId, String conversationId) {
        return admin.messages(userId, conversationId);
    }

    boolean rename(String userId, String conversationId, String title) {
        return admin.rename(userId, conversationId, title);
    }

    boolean remove(String userId, String conversationId) {
        return admin.remove(userId, conversationId);
    }

    boolean removeMessage(String userId, String conversationId, String messageId) {
        return admin.removeMessage(userId, conversationId, messageId);
    }

    int titleMaxChars() {
        return admin.titleMaxChars();
    }

    ConversationCatalog catalog() {
        return repository.catalog();
    }

    long parseFailures() {
        return repository.parseFailures();
    }

    long persistFailures() {
        return repository.persistFailures();
    }

    private void append(String userId, String conversationId, StoredMessage message) {
        boolean needCompact;
        synchronized (repository) {
            ConversationRecord record = repository.load(conversationId);
            if (record == null) {
                record = new ConversationRecord();
                record.userId = userId;
                record.createdAt = message.createdAt;
            } else if (!userId.equals(record.userId)) {
                // 归属校验属信任边界：他人会话一律不写入，也不暴露其存在
                logger.warn("conversation " + conversationId + " does not belong to user, message skipped");
                return;
            }
            if (record.archived) record.archived = false; // 继续对话即重新激活，原摘要仍作背景保留
            if (record.messages == null) record.messages = new ArrayList<>();
            record.messages.add(message);
            trimToHardLimit(conversationId, record);
            record.updatedAt = message.createdAt;
            repository.persist(conversationId, record);
            quota.enforce(userId);

            CatalogEntry entry = repository.catalog().get(conversationId);
            needCompact = entry != null && !entry.archived && entry.messageCount > limits.softMessages;
        }
        if (needCompact) compactor.submit(conversationId);
    }

    /**
     * 单会话条数兜底：压缩滞后时先把被裁消息折进摘要再移除，避免历史从 KV 与摘要中同时消失
     */
    private void trimToHardLimit(String conversationId, ConversationRecord record) {
        int excess = record.messages.size() - limits.hardMessages;
        if (excess <= 0) return;
        List<StoredMessage> dropped = new ArrayList<>(record.messages.subList(0, excess));
        record.messages.subList(0, excess).clear();
        record.summary = MemoryTexts.spliceSummary(record.summary, dropped, limits.summaryMaxChars);
        logger.warn("conversation " + conversationId + " exceeded hard message limit before compaction finished, "
                + dropped.size() + " oldest messages folded into summary");
    }

    private StoredMessage message(String role, String content, String messageId) {
        StoredMessage message = new StoredMessage();
        message.id = messageId;
        message.role = role;
        message.content = MemoryTexts.truncate(content, limits.messageMaxChars);
        message.createdAt = System.currentTimeMillis();
        return message;
    }
}
