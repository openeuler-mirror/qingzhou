package qingzhou.ai.memory.impl;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import qingzhou.llm.ChatMemory;
import qingzhou.store.Store;
import qingzhou.store.StoreFactory;

public class ConversationStoreImplTest {
    @AfterMethod
    public void tearDown() {
        MemoryTestSupport.shutdownAll();
    }

    @Test
    public void overheadExceedsBudget_getMessageList_keepsRecentTurn() throws Exception {
        ConversationStoreImpl store = MemoryTestSupport.newStore(MemoryTestSupport.config(100));
        String longText = MemoryTestSupport.repeat("内容", 1500); // 单条 3000 字符，两轮即超出最小预算
        store.storeUserMessage("admin", "conv-long", longText, "user-1");
        store.storeAssistantMessage("admin", "conv-long", longText, "assistant-1");
        store.storeUserMessage("admin", "conv-long", longText, "user-2");
        store.storeAssistantMessage("admin", "conv-long", longText, "assistant-2");

        List<ChatMemory.Message> messages = store.getMessageList("admin", "conv-long", 0);

        Assert.assertEquals(messages.size(), 2, "预算耗尽时仍须保留最近 1 轮问答");
        Assert.assertTrue(messages.get(0) instanceof ChatMemory.UserMessage);
        Assert.assertTrue(messages.get(1) instanceof ChatMemory.AssistantMessage);
    }

    @Test
    public void historyMaxMessagesBelowOneTurn_getMessageList_keepsRecentTurn() throws Exception {
        Map<String, String> config = MemoryTestSupport.config(100);
        config.put("history_max_messages", "1"); // 条数护栏配得比一轮还小
        ConversationStoreImpl store = MemoryTestSupport.newStore(config);
        store.storeUserMessage("admin", "conv-1", "问题", "u-1");
        store.storeAssistantMessage("admin", "conv-1", "回答", "a-1");

        List<ChatMemory.Message> messages = store.getMessageList("admin", "conv-1", 0);

        Assert.assertEquals(messages.size(), 2, "条数护栏不能击穿最近 1 轮问答的保底");
    }

    @Test
    public void otherUsersConversation_isInvisibleAndImmutable() throws Exception {
        ConversationStoreImpl store = MemoryTestSupport.newStore(MemoryTestSupport.config(100));
        store.storeUserMessage("alice", "conv-owner", "私有问题", "u-owner");

        Assert.assertTrue(store.getMessageList("bob", "conv-owner", 0).isEmpty());
        Assert.assertTrue(store.listMessages("bob", "conv-owner").isEmpty());
        Assert.assertFalse(store.rename("bob", "conv-owner", "篡改"));
        Assert.assertFalse(store.remove("bob", "conv-owner"));
        Assert.assertFalse(store.removeMessage("bob", "conv-owner", "任意"));

        Assert.assertEquals(store.listMessages("alice", "conv-owner").size(), 1);
    }

    @Test
    public void persistFailure_storeUserMessage_throws() throws Exception {
        StoreFactory failing = new StoreFactory() {
            @Override
            public Store buildMemoryStore() {
                return new FailingStore();
            }

            @Override
            public Store buildFileStore(File baseDir) {
                return new FailingStore();
            }
        };
        ConversationStoreImpl store = MemoryTestSupport.newStore(MemoryTestSupport.config(100), failing);

        try {
            store.storeUserMessage("admin", "conv-fail", "问题", "u-fail");
            Assert.fail("主写入落库失败必须上抛，不得静默成功");
        } catch (IllegalStateException expected) {
            // 预期路径
        }
    }

    @Test
    public void messagesExceedHardLimit_historyStillInjected() throws Exception {
        Map<String, String> config = MemoryTestSupport.config(100);
        config.put("conversation_soft_messages", "4");
        config.put("conversation_hard_messages", "6");
        config.put("conversation_keep_messages", "2");
        ConversationStoreImpl store = MemoryTestSupport.newStore(config);
        for (int i = 1; i <= 8; i++) store.storeUserMessage("admin", "conv-hard", "第" + i + "个问题", "u-" + i);

        String injected = store.getMessageList("admin", "conv-hard", 0).stream()
                .map(ChatMemory.Message::content).collect(Collectors.joining("\n"));

        Assert.assertTrue(injected.contains("第1个问题"), "兜底裁剪须把被裁消息折进摘要，不得让历史凭空消失");
    }

    private static final class FailingStore implements Store {
        @Override
        public void put(String key, String value) {
            throw new IllegalStateException("disk full");
        }

        @Override
        public String get(String key) {
            return null;
        }

        @Override
        public void delete(String key) {
        }

        @Override
        public boolean contains(String key) {
            return false;
        }

        @Override
        public Set<String> keys() {
            return Collections.emptySet();
        }
    }
}
