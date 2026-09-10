package qingzhou.ai.memory;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import qingzhou.json.Json;
import qingzhou.json.impl.JsonImpl;
import qingzhou.llm.HistoryMessage;
import qingzhou.logger.Logger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * 文件记忆实现的读写与治理（列表/改名/删除）测试，含路径遍历与横向越权防护。
 */
public class FileChatMemoryTest {
    private Json json;
    private Logger logger;

    @BeforeClass
    public void setUp() throws Exception {
        // 构造器读取实例目录，测试环境指向临时目录
        if (System.getProperty("qingzhou.instance") == null) {
            System.setProperty("qingzhou.instance", Files.createTempDirectory("qingzhou-test").toString());
        }
        JsonImpl jsonImpl = new JsonImpl();
        jsonImpl.init();
        json = jsonImpl;
        logger = stub(Logger.class);
    }

    private static <T> T stub(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> {
                    Class<?> rt = method.getReturnType();
                    if (rt == boolean.class) return false;
                    if (rt == int.class) return 0;
                    if (rt == long.class) return 0L;
                    return null;
                });
    }

    @Test
    public void appendAndListMessages_returnFullHistoryInOrder() {
        FileChatMemory memory = new FileChatMemory(json, logger);
        memory.appendUserMessage("u1", "conv-11111111", "hi");
        memory.appendAssistantMessage("u1", "conv-11111111", "m-1", "hello", 1, 2, 3);

        List<HistoryMessage> messages = memory.listMessages("u1", "conv-11111111");
        assertEquals(messages.size(), 2);
        assertEquals(messages.get(0).role, "user");
        assertEquals(messages.get(0).content, "hi");
        assertEquals(messages.get(1).content, "hello");
    }

    @Test
    public void recentHistory_truncateToMaxMessages() {
        FileChatMemory memory = new FileChatMemory(json, logger);
        memory.appendUserMessage("u2", "conv-22222222", "m1");
        memory.appendAssistantMessage("u2", "conv-22222222", "m-2", "m2", 0, 0, 0);
        memory.appendUserMessage("u2", "conv-22222222", "m3");

        List<HistoryMessage> history = memory.recentHistory("u2", "conv-22222222", 2);
        assertEquals(history.size(), 2);
        assertEquals(history.get(0).content, "m2");
        assertEquals(history.get(1).content, "m3");
    }

    @Test
    public void listRenameDelete_governLifecycle() throws Exception {
        FileChatMemory memory = new FileChatMemory(json, logger);
        memory.appendUserMessage("u3", "conv-33333333", "first");
        Thread.sleep(10); // 保证两会话 updatedAt 可区分
        memory.appendUserMessage("u3", "conv-44444444", "later");

        List<ConversationSummary> summaries = memory.listConversations("u3");
        assertEquals(summaries.size(), 2);
        assertEquals(summaries.get(0).conversationId, "conv-44444444"); // 最近更新在前

        assertTrue(memory.renameConversation("u3", "conv-33333333", "标题A"));
        assertEquals(memory.listConversations("u3").get(0).title, "标题A"); // 改名后置顶

        assertTrue(memory.deleteConversation("u3", "conv-44444444"));
        assertTrue(memory.listMessages("u3", "conv-44444444").isEmpty());
        assertFalse(memory.deleteConversation("u3", "conv-44444444")); // 重复删除返回 false
        assertFalse(memory.renameConversation("u3", "conv-44444444", "x")); // 不存在返回 false
    }

    @Test
    public void invalidOrForeignAccess_returnEmptyOrFalse() {
        FileChatMemory memory = new FileChatMemory(json, logger);
        memory.appendUserMessage("u4", "conv-55555555", "secret");

        // 非法 id（含路径遍历）：治理方法一律拒绝
        assertFalse(memory.deleteConversation("u4", "../../etc/passwd"));
        assertFalse(memory.renameConversation("u4", "../../etc/passwd", "x"));
        assertTrue(memory.listMessages("u4", "../../etc/passwd").isEmpty());

        // 横向越权：其他用户视角下会话不存在
        assertTrue(memory.listMessages("other", "conv-55555555").isEmpty());
        assertFalse(memory.deleteConversation("other", "conv-55555555"));
        assertFalse(memory.renameConversation("other", "conv-55555555", "x"));
    }
}
