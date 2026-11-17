package qingzhou.ai.memory.impl;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class ConversationAdminTest {
    @AfterMethod
    public void tearDown() {
        MemoryTestSupport.shutdownAll();
    }

    @Test
    public void listConversations_mostRecentlyUpdatedFirst() throws Exception {
        ConversationStoreImpl store = MemoryTestSupport.newStore(MemoryTestSupport.config(100));
        store.rename("admin", "c1", "第一");
        Thread.sleep(10); // updatedAt 只有毫秒精度
        store.rename("admin", "c2", "第二");

        List<ConversationSummary> conversations = store.listConversations("admin");

        Assert.assertEquals(conversations.size(), 2);
        Assert.assertEquals(conversations.get(0).conversationId, "c2");
        Assert.assertEquals(conversations.get(0).title, "第二");
    }

    @Test
    public void renameRemoveAndDeleteMessage_lifecycle() throws Exception {
        ConversationStoreImpl store = MemoryTestSupport.newStore(MemoryTestSupport.config(100));
        String userMessageId = "u-1";
        store.storeUserMessage("admin", "conv-1", "问题", userMessageId);
        store.storeAssistantMessage("admin", "conv-1", "回答", "a-1");

        Assert.assertTrue(store.rename("admin", "conv-1", "标题"));
        Assert.assertEquals(store.listConversations("admin").get(0).title, "标题");

        Assert.assertTrue(store.removeMessage("admin", "conv-1", userMessageId));
        Assert.assertEquals(store.listMessages("admin", "conv-1").size(), 1);

        Assert.assertTrue(store.remove("admin", "conv-1"));
        Assert.assertTrue(store.listConversations("admin").isEmpty());
    }

    @Test
    public void renameWithoutTitle_createsNothing() throws Exception {
        ConversationStoreImpl store = MemoryTestSupport.newStore(MemoryTestSupport.config(100));

        Assert.assertFalse(store.rename("admin", "ghost", null));
        Assert.assertTrue(store.listConversations("admin").isEmpty());
    }

    @Test
    public void removeLastMessage_removesConversation() throws Exception {
        ConversationStoreImpl store = MemoryTestSupport.newStore(MemoryTestSupport.config(100));
        String messageId = "u-single";
        store.storeUserMessage("admin", "conv-single", "问题", messageId);

        Assert.assertTrue(store.removeMessage("admin", "conv-single", messageId));
        Assert.assertTrue(store.listConversations("admin").isEmpty());
        Assert.assertTrue(store.listMessages("admin", "conv-single").isEmpty());
    }
}
