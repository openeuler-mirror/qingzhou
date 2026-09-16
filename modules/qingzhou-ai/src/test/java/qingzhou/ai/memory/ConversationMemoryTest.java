package qingzhou.ai.memory;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import qingzhou.json.Json;
import qingzhou.json.impl.JsonImpl;
import qingzhou.kv.KeyValueStore;
import qingzhou.kv.impl.FileKeyValueStore;
import qingzhou.kv.impl.MemoryKeyValueStore;
import qingzhou.llm.HistoryMessage;
import qingzhou.logger.Logger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * 会话存储与索引行为测试（重构核心行为等价点）：历史窗口、条数/内容截断、
 * 索引配额挤出、排序、改名/删除、归属判定；文件后端冒烟验证持久化链路。
 */
public class ConversationMemoryTest {
    private Json json;
    private Logger logger;

    @BeforeMethod
    public void setup() throws Exception {
        File dir = Files.createTempDirectory("qingzhou-ai-test").toFile();
        dir.deleteOnExit();
        System.setProperty("qingzhou.instance", dir.getAbsolutePath()); // 文件后端数据目录
        json = new JsonImpl();
        ((JsonImpl) json).init();
        logger = noopLogger();
    }

    @Test
    public void storeHistoryAndLimits() {
        ConversationStore store = new ConversationStore(new MemoryKeyValueStore(), json, logger);
        store.appendUser("c1", "hi");
        store.appendAssistant("c1", "m1", "hello", 3, 5, 8);
        List<HistoryMessage> history = store.listMessages("c1");
        assertEquals(history.size(), 2);
        assertEquals(history.get(0).role, "user");
        assertEquals(history.get(0).content, "hi");
        assertEquals(history.get(1).role, "assistant");
        assertEquals(history.get(1).content, "hello");

        // 历史窗口：recentHistory 取尾部（时间正序）
        List<HistoryMessage> window = store.recentHistory("c1", 1);
        assertEquals(window.size(), 1);
        assertEquals(window.get(0).content, "hello");

        // 单条内容截断 8000 字符
        char[] big = new char[9000];
        java.util.Arrays.fill(big, 'x');
        store.appendUser("c1", new String(big));
        assertEquals(store.listMessages("c1").get(2).content.length(), 8000);
    }

    @Test
    public void storeMessageCap() {
        ConversationStore store = new ConversationStore(new MemoryKeyValueStore(), json, logger);
        for (int i = 0; i < 210; i++) {
            store.appendUser("c1", "msg-" + i);
        }
        List<HistoryMessage> messages = store.listMessages("c1");
        assertEquals(messages.size(), 200);
        assertEquals(messages.get(0).content, "msg-10"); // 最旧 10 条被挤出
        assertEquals(messages.get(199).content, "msg-209");
    }

    @Test
    public void storeEmptyContentSkipped() {
        ConversationStore store = new ConversationStore(new MemoryKeyValueStore(), json, logger);
        store.appendUser("c1", "q");
        store.appendAssistant("c1", "m1", "", 0, 0, 0); // 空回复不进历史
        List<HistoryMessage> messages = store.listMessages("c1");
        assertEquals(messages.size(), 1);
        assertEquals(messages.get(0).role, "user");
    }

    @Test
    public void storeDelete() {
        ConversationStore store = new ConversationStore(new MemoryKeyValueStore(), json, logger);
        store.appendUser("c1", "hi");
        assertTrue(store.exists("c1"));
        assertTrue(store.delete("c1"));
        assertFalse(store.exists("c1"));
        assertTrue(store.listMessages("c1").isEmpty());
    }

    @Test
    public void indexRegisterQuotaAndList() {
        ConversationIndex index = new ConversationIndex(new MemoryKeyValueStore(), json, logger);
        for (int i = 0; i < 50; i++) {
            assertTrue(index.register("u1", "conv-" + i).isEmpty());
        }
        assertTrue(index.contains("u1", "conv-0"));
        assertTrue(index.contains("u1", "conv-49"));

        // 配额 50：注册第 51 个挤出 updatedAt 最旧者
        List<String> evicted = index.register("u1", "conv-50");
        assertEquals(evicted.size(), 1);
        assertEquals(evicted.get(0), "conv-0");
        assertFalse(index.contains("u1", "conv-0"));
        assertTrue(index.contains("u1", "conv-50"));

        assertTrue(index.register("u1", "conv-50").isEmpty()); // 幂等：重复注册不挤出
    }

    @Test
    public void indexUserIsolation() {
        ConversationIndex index = new ConversationIndex(new MemoryKeyValueStore(), json, logger);
        index.register("alice", "conv-a");
        assertTrue(index.contains("alice", "conv-a"));
        assertFalse(index.contains("bob", "conv-a")); // 归属隔离：他人索引不可见
        assertTrue(index.list("bob").isEmpty());
    }

    @Test
    public void indexTouchSort() throws Exception {
        ConversationIndex index = new ConversationIndex(new MemoryKeyValueStore(), json, logger);
        index.register("u1", "c1");
        Thread.sleep(5);
        index.register("u1", "c2");
        assertEquals(index.list("u1").get(0).conversationId, "c2"); // 新会话在前
        Thread.sleep(5);
        index.touch("u1", "c1");
        assertEquals(index.list("u1").get(0).conversationId, "c1"); // 活跃会话靠前
    }

    @Test
    public void indexRenameAndRemove() {
        ConversationIndex index = new ConversationIndex(new MemoryKeyValueStore(), json, logger);
        index.register("u1", "c1");
        assertFalse(index.rename("u1", "missing", "t")); // 未命中 false（调用方 404）
        assertTrue(index.rename("u1", "c1", "标题"));
        assertEquals(index.list("u1").get(0).title, "标题");
        assertTrue(index.rename("u1", "c1", null)); // null = 清除标题（未命名）
        assertNull(index.list("u1").get(0).title);

        assertTrue(index.remove("u1", "c1"));
        assertFalse(index.contains("u1", "c1"));
        assertFalse(index.remove("u1", "c1")); // 幂等：未命中 false
    }

    @Test
    public void fileBackendSmoke() {
        KeyValueStore kv = new FileKeyValueStore();
        ConversationStore store = new ConversationStore(kv, json, logger);
        ConversationIndex index = new ConversationIndex(kv, json, logger);
        index.register("u1", "c1");
        store.appendUser("c1", "persisted");
        assertEquals(store.listMessages("c1").get(0).content, "persisted");
        assertTrue(index.contains("u1", "c1"));
        assertEquals(store.recentHistory("c1", 20).get(0).content, "persisted");
    }

    private Logger noopLogger() {
        return new Logger() {
            @Override public boolean isDebugEnabled() { return false; }
            @Override public void debug(String msg) { }
            @Override public void debug(String msg, Throwable t) { }
            @Override public boolean isInfoEnabled() { return false; }
            @Override public void info(String msg) { }
            @Override public void info(String msg, Throwable t) { }
            @Override public boolean isWarnEnabled() { return false; }
            @Override public void warn(String msg) { }
            @Override public void warn(String msg, Throwable t) { }
            @Override public boolean isErrorEnabled() { return false; }
            @Override public void error(String msg) { }
            @Override public void error(String msg, Throwable t) { }
        };
    }
}
