package qingzhou.ai.memory.impl;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import qingzhou.llm.ChatMemory;

public class ConversationQuotaTest {
    @AfterMethod
    public void tearDown() {
        MemoryTestSupport.shutdownAll();
    }

    @Test
    public void perUserLimit_oldestConversationArchivedNotDeleted() throws Exception {
        ConversationStoreImpl store = MemoryTestSupport.newStore(MemoryTestSupport.config(2));
        store.storeUserMessage("admin", "c1", "一", "u-c1");
        store.storeUserMessage("admin", "c2", "二", "u-c2");
        store.storeUserMessage("admin", "c3", "三", "u-c3");

        Assert.assertEquals(store.listConversations("admin").size(), 3, "归档不删除会话");
        List<HistoryMessage> oldest = store.listMessages("admin", "c1");
        Assert.assertEquals(oldest.size(), 1);
        Assert.assertTrue(oldest.get(0).content.startsWith("（历史"), "最旧会话应只剩摘要视图");
        Assert.assertEquals(store.listMessages("admin", "c3").get(0).content, "三");
    }

    @Test
    public void archivedConversation_reactivatesWithSummaryAsBackground() throws Exception {
        ConversationStoreImpl store = MemoryTestSupport.newStore(MemoryTestSupport.config(1));
        store.storeUserMessage("admin", "c1", "旧", "u-old");
        store.storeUserMessage("admin", "c2", "新", "u-new"); // c1 被归档

        store.storeUserMessage("admin", "c1", "续", "u-resume"); // 继续对话即重新激活

        Assert.assertEquals(store.listMessages("admin", "c1").get(0).content, "续");
        List<ChatMemory.Message> injected = store.getMessageList("admin", "c1", 0);
        Assert.assertTrue(injected.get(0) instanceof ChatMemory.SummaryMessage, "原摘要仍作背景保留");
        Assert.assertEquals(injected.get(1).content(), "续");
    }

    @Test
    public void globalByteLimit_archivesActiveConversation() throws Exception {
        Map<String, String> config = MemoryTestSupport.config(100);
        config.put("memory_max_bytes", "1"); // 任何记录都超限，验证全局字节阈值先走归档而非直接删除
        ConversationStoreImpl store = MemoryTestSupport.newStore(config);
        store.storeUserMessage("admin", "c-byte", "一", "u-byte");

        List<HistoryMessage> messages = store.listMessages("admin", "c-byte");
        Assert.assertEquals(messages.size(), 1);
        Assert.assertTrue(messages.get(0).content.startsWith("（历史"));
    }

    @Test
    public void fileBackend_rebuildRestoresConversations() throws Exception {
        Map<String, String> config = MemoryTestSupport.config(100);
        config.put("memory_store", "file");
        config.put("qingzhou.instance", new File("target").getAbsolutePath());
        config.put("memory_store_dir", "test-conversations-" + UUID.randomUUID());

        ConversationStoreImpl first = MemoryTestSupport.newStore(config);
        first.storeUserMessage("admin", "conv-file", "问题", "u-file");
        first.storeAssistantMessage("admin", "conv-file", "回答", "a-file");
        MemoryTestSupport.shutdownAll();

        ConversationStoreImpl second = MemoryTestSupport.newStore(config);

        Assert.assertEquals(second.listConversations("admin").size(), 1);
        Assert.assertEquals(second.listMessages("admin", "conv-file").size(), 2);
        Assert.assertEquals(second.getMessageList("admin", "conv-file", 0).size(), 2);
    }
}
