package qingzhou.ai.memory;

import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import qingzhou.llm.ChatMemory;

/**
 * 压缩链路只经由公开接口与外部配置驱动：调低 soft 阈值让写入自动触发压缩任务，
 * 轮询注入上下文的消息列表（1 条摘要 + 保留的历史）断言结果，不深入压缩器内部方法。
 */
public class ConversationCompactorTest {
    private static final long AWAIT_TIMEOUT_MS = 5000L;

    @AfterMethod
    public void tearDown() {
        MemoryTestSupport.shutdownAll();
    }

    @Test
    public void messagesOverSoftThreshold_compacted_keepsRecentAndWritesSummary() throws Exception {
        ConversationStore store = compactableStore(2, 24000, 9);
        appendTurns(store, "conv-c", 5); // 10 条消息，写入即触发压缩任务

        List<ChatMemory.Message> injected = awaitInjected(store, "conv-c", 2);

        Assert.assertTrue(injected.get(0) instanceof ChatMemory.SummaryMessage, "摘要须置于历史之前");
        Assert.assertTrue(injected.get(0).content().contains("第1个问题"), "摘要须覆盖被移除的早期消息");
        Assert.assertTrue(injected.get(0).content().contains("第4个回答"), "摘要须覆盖到最后一条待压缩消息");
        Assert.assertTrue(injected.get(1) instanceof ChatMemory.UserMessage, "应保留最近的用户消息");
        Assert.assertEquals(injected.get(1).content(), "第5个问题");
        Assert.assertTrue(injected.get(2) instanceof ChatMemory.AssistantMessage, "应保留最近的助手回答");
        Assert.assertEquals(injected.get(2).content(), "第5个回答");
    }

    @Test
    public void withoutModelFactory_summaryFallsBackToConcatenation() throws Exception {
        ConversationStore store = compactableStore(2, 24000, 9);
        appendTurns(store, "conv-c", 5);

        List<ChatMemory.Message> injected = awaitInjected(store, "conv-c", 2);

        Assert.assertTrue(injected.get(0).content().startsWith("用户："), "未配置模型时应回退为拼接式摘要");
    }

    @Test
    public void tinyBatchBudget_compactionStillCoversAllMessages() throws Exception {
        ConversationStore store = compactableStore(2, 12, 9); // 每批仅容纳约 1 条，强制多批压缩
        appendTurns(store, "conv-c", 5);

        List<ChatMemory.Message> injected = awaitInjected(store, "conv-c", 2);

        Assert.assertEquals(injected.size(), 3, "分批压缩后仍应只保留 1 条摘要 + keep 条最近消息");
        String summary = injected.get(0).content();
        Assert.assertTrue(summary.contains("第1个问题"), "分批压缩须覆盖最早的待压缩消息");
        Assert.assertTrue(summary.contains("第4个回答"), "分批压缩须覆盖完全部待压缩消息");
    }

    /**
     * 外部配置驱动：调低 soft 阈值使写入自动触发压缩，keep 与批预算决定压缩行为
     */
    private static ConversationStore compactableStore(int keepMessages, int inputMaxChars, int softMessages) throws Exception {
        Map<String, String> config = MemoryTestSupport.config(100);
        config.put("conversation_soft_messages", String.valueOf(softMessages));
        config.put("conversation_keep_messages", String.valueOf(keepMessages));
        config.put("compaction_input_max_chars", String.valueOf(inputMaxChars));
        return MemoryTestSupport.newStore(config);
    }

    private static void appendTurns(ConversationStore store, String conversationId, int turns) {
        for (int i = 1; i <= turns; i++) {
            store.storeUserMessage(conversationId, "admin", "第" + i + "个问题");
            store.storeAssistantMessage(conversationId, "admin", "第" + i + "个回答", "a-" + i);
        }
    }

    /**
     * 轮询等待压缩完成：注入列表收敛为「1 条摘要 + expectedMessages 条历史」
     */
    private static List<ChatMemory.Message> awaitInjected(ConversationStore store, String conversationId,
                                                          int expectedMessages) throws InterruptedException {
        long deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            List<ChatMemory.Message> injected = store.getMessageList("admin", conversationId, 0);
            if (injected.size() == expectedMessages + 1 && injected.get(0) instanceof ChatMemory.SummaryMessage) {
                return injected;
            }
            Thread.sleep(50);
        }
        Assert.fail("压缩未在期限内完成：期望 1 条摘要 + " + expectedMessages + " 条历史");
        return null; // 不可达：Assert.fail 必然抛出
    }
}
