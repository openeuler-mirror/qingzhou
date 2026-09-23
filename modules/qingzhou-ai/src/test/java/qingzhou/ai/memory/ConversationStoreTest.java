package qingzhou.ai.memory;

import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import qingzhou.llm.ChatMemory;

public class ConversationStoreTest {
    @AfterMethod
    public void tearDown() {
        MemoryTestSupport.shutdownAll();
    }

    @Test
    public void overPerUserLimit_append_oldestConversationIsArchived() throws Exception {
        ConversationStore store = MemoryTestSupport.newStore(MemoryTestSupport.config(2));
        store.storeUserMessage("conv-1", "admin", "第一条会话的消息"); // 最旧，应被归档
        store.storeUserMessage("conv-2", "admin", "第二条会话的消息");
        store.storeAssistantMessage("conv-2", "admin", "第二条会话的回答", "a-2");
        store.storeUserMessage("conv-3", "admin", "第三条会话的消息");
        store.storeAssistantMessage("conv-3", "admin", "第三条会话的回答", "a-3");
        store.storeUserMessage("conv-3", "admin", "第三条会话的追问");

        Assert.assertEquals(store.listMessages("admin", "conv-1").size(), 1, "被归档的会话应只剩一条摘要消息");
        Assert.assertEquals(store.listMessages("admin", "conv-2").size(), 2, "未超限的会话应保留原消息");
        Assert.assertEquals(store.listMessages("admin", "conv-3").size(), 3, "未超限的会话应保留原消息");
        Assert.assertEquals(store.listConversations("admin").size(), 3, "归档会话不应从列表中消失");
    }

    @Test
    public void archive_overQuota_clearsBodyAndKeepsUpdatedAt() throws Exception {
        ConversationStore store = MemoryTestSupport.newStore(MemoryTestSupport.config(1));
        store.storeUserMessage("conv-old", "admin", "旧会话内容");
        Map<String, Object> before = MemoryTestSupport.recordOf(store, "conv-old");

        store.storeUserMessage("conv-new", "admin", "新会话内容");

        Map<String, Object> archived = MemoryTestSupport.recordOf(store, "conv-old");
        Assert.assertEquals(archived.get("archived"), Boolean.TRUE, "超限会话应被归档");
        Assert.assertNull(archived.get("messages"), "归档须清空正文");
        Assert.assertNotNull(archived.get("summary"), "归档须留下摘要");
        Assert.assertEquals(archived.get("updatedAt"), before.get("updatedAt"), "归档不应更新 updatedAt");
    }

    @Test
    public void overheadExceedsBudget_getMessageList_keepsRecentTurn() throws Exception {
        ConversationStore store = MemoryTestSupport.newStore(MemoryTestSupport.config(100));
        String longText = MemoryTestSupport.repeat("内容", 1500); // 单条 3000 字符，两轮即超出最小预算
        store.storeUserMessage("conv-long", "admin", longText);
        store.storeAssistantMessage("conv-long", "admin", longText, "assistant-1");
        store.storeUserMessage("conv-long", "admin", longText);
        store.storeAssistantMessage("conv-long", "admin", longText, "assistant-2");

        List<ChatMemory.Message> messages = store.getMessageList("admin", "conv-long", 999999);

        Assert.assertEquals(messages.size(), 2, "预算被开销吃光时仍须保留最近 1 轮问答");
        Assert.assertTrue(messages.get(0) instanceof ChatMemory.UserMessage);
        Assert.assertTrue(messages.get(1) instanceof ChatMemory.AssistantMessage);
    }

    @Test
    public void legacyRecordWithoutNewFields_listMessages_readsNormally() throws Exception {
        ConversationStore store = MemoryTestSupport.newStore(MemoryTestSupport.config(100));
        // 升级前写入的记录：没有 summary / summaryUpTo / archived 字段
        MemoryTestSupport.kvOf(store).put("conv-legacy",
                "{\"id\":\"conv-legacy\",\"userId\":\"admin\",\"title\":null,\"createdAt\":1,\"updatedAt\":2,"
                        + "\"messages\":[{\"id\":\"m1\",\"role\":\"user\",\"content\":\"旧格式消息\",\"createdAt\":1}]}");

        Assert.assertEquals(store.listMessages("admin", "conv-legacy").size(), 1, "旧格式记录应能正常读取");
    }

    @Test
    public void historyMaxMessagesBelowOneTurn_getMessageList_keepsRecentTurn() throws Exception {
        Map<String, String> config = MemoryTestSupport.config(100);
        config.put("history_max_messages", "1"); // 条数护栏配得比一轮还小
        ConversationStore store = MemoryTestSupport.newStore(config);
        store.storeUserMessage("conv-1", "admin", "问题");
        store.storeAssistantMessage("conv-1", "admin", "回答", "a-1");

        List<ChatMemory.Message> messages = store.getMessageList("admin", "conv-1", 999999);

        Assert.assertEquals(messages.size(), 2, "条数护栏不能击穿最近 1 轮问答的保底");
    }

    @Test
    public void archive_titledConversationWithoutMessages_keepsRecord() throws Exception {
        ConversationStore store = MemoryTestSupport.newStore(MemoryTestSupport.config(1));
        store.rename("admin", "conv-title", "只有标题的会话"); // 首条消息落库前先命名
        store.storeUserMessage("conv-2", "admin", "触发配额");

        Map<String, Object> record = MemoryTestSupport.recordOf(store, "conv-title");
        Assert.assertEquals(record.get("archived"), Boolean.TRUE, "只有标题的会话也应归档而非销毁");
        Assert.assertEquals(record.get("title"), "只有标题的会话", "归档须保留标题");
    }

    @Test
    public void summaryCountedInBudget_getMessageList_trimsMoreHistory() throws Exception {
        Map<String, String> config = MemoryTestSupport.config(100);
        config.put("history_total_budget", "1000");
        config.put("history_output_reserve", "0");
        config.put("history_min_chars", "100");
        ConversationStore store = MemoryTestSupport.newStore(config);

        String body = MemoryTestSupport.repeat("字", 200);
        StringBuilder messages = new StringBuilder("[");
        for (int i = 1; i <= 4; i++) {
            if (i > 1) messages.append(',');
            messages.append("{\"id\":\"m").append(i).append("\",\"role\":\"user\",\"content\":\"")
                    .append(body).append("\",\"createdAt\":").append(i).append('}');
        }
        messages.append(']');
        MemoryTestSupport.kvOf(store).put("conv-sum",
                "{\"id\":\"conv-sum\",\"userId\":\"admin\",\"summary\":\""
                        + MemoryTestSupport.repeat("摘", 500) + "\",\"messages\":" + messages + "}");

        List<ChatMemory.Message> got = store.getMessageList("admin", "conv-sum", 0);

        // 预算 1000 扣掉 500 字摘要后仅够 2 条历史，另有 1 条摘要消息
        Assert.assertEquals(got.size(), 3, "摘要占用的字符须从历史预算中扣除");
    }

    @Test
    public void appendToArchivedConversation_reactivatesAndKeepsTurns() throws Exception {
        ConversationStore store = MemoryTestSupport.newStore(MemoryTestSupport.config(1));
        store.storeUserMessage("conv-1", "admin", "旧内容");
        store.storeUserMessage("conv-2", "admin", "触发归档");
        Assert.assertEquals(MemoryTestSupport.recordOf(store, "conv-1").get("archived"), Boolean.TRUE);

        store.storeUserMessage("conv-1", "admin", "继续对话"); // 归档会话不可续聊则新消息会写而不可读

        Map<String, Object> record = MemoryTestSupport.recordOf(store, "conv-1");
        Assert.assertEquals(record.get("archived"), Boolean.FALSE, "继续对话应重新激活会话");
        Assert.assertEquals(((List<?>) record.get("messages")).size(), 1, "新消息须可被读取");
        Assert.assertEquals(store.listMessages("admin", "conv-1").size(), 1, "重新激活后不再返回归档摘要视图");
    }
}
