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
    public void historyMaxMessagesBelowOneTurn_getMessageList_keepsRecentTurn() throws Exception {
        Map<String, String> config = MemoryTestSupport.config(100);
        config.put("history_max_messages", "1"); // 条数护栏配得比一轮还小
        ConversationStore store = MemoryTestSupport.newStore(config);
        store.storeUserMessage("conv-1", "admin", "问题");
        store.storeAssistantMessage("conv-1", "admin", "回答", "a-1");

        List<ChatMemory.Message> messages = store.getMessageList("admin", "conv-1", 999999);

        Assert.assertEquals(messages.size(), 2, "条数护栏不能击穿最近 1 轮问答的保底");
    }
}
