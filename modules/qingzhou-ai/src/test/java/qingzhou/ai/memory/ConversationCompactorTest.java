package qingzhou.ai.memory;

import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class ConversationCompactorTest {
    @AfterMethod
    public void tearDown() {
        MemoryTestSupport.shutdownAll();
    }

    @Test
    public void compact_messagesOverKeep_keepsRecentAndWritesSummary() throws Exception {
        ConversationStore store = compactableStore(2, 24000);
        appendTurns(store, "conv-c", 5); // 10 条消息

        MemoryTestSupport.compactorOf(store).compact("conv-c");

        Map<String, Object> record = MemoryTestSupport.recordOf(store, "conv-c");
        Assert.assertEquals(((List<?>) record.get("messages")).size(), 2, "压缩后应只保留 keep 条最近消息");
        Assert.assertNotNull(record.get("summary"), "压缩须写入摘要");
        Assert.assertEquals(record.get("summaryUpTo"), "a-4", "summaryUpTo 应指向本次摘要覆盖的最后一条消息");
    }

    @Test
    public void compact_withoutModelFactory_writesConcatenatedSummary() throws Exception {
        ConversationStore store = compactableStore(2, 24000);
        appendTurns(store, "conv-c", 5);

        MemoryTestSupport.compactorOf(store).compact("conv-c");

        String summary = (String) MemoryTestSupport.recordOf(store, "conv-c").get("summary");
        Assert.assertTrue(summary.startsWith("用户："), "未配置模型时应回退为拼接式摘要");
    }

    @Test
    public void compact_inputOverBatchLimit_summarizesAllBatches() throws Exception {
        ConversationStore store = compactableStore(2, 12); // 每批仅容纳 1~2 条，强制多批
        appendTurns(store, "conv-c", 5);

        MemoryTestSupport.compactorOf(store).compact("conv-c");

        Map<String, Object> record = MemoryTestSupport.recordOf(store, "conv-c");
        Assert.assertEquals(((List<?>) record.get("messages")).size(), 2, "分批压缩后仍应只保留 keep 条");
        Assert.assertEquals(record.get("summaryUpTo"), "a-4", "分批压缩须覆盖完全部待压缩消息");
    }

    private static ConversationStore compactableStore(int keepMessages, int inputMaxChars) throws Exception {
        Map<String, String> config = MemoryTestSupport.config(100);
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
}
