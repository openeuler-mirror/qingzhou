package qingzhou.kv;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import qingzhou.kv.impl.FileKeyValueStore;
import qingzhou.kv.impl.MemoryKeyValueStore;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * KV 契约测试：文件/内存双实现跑同一套用例（CRUD、update 原子读改写、契约校验、单值上限）；
 * 并发用例验证 update 同键互斥——追加竞态必须收敛，丢一次读改写即契约破坏。
 */
public class KeyValueStoreTest {
    @BeforeClass
    public void setup() throws Exception {
        File dir = Files.createTempDirectory("qingzhou-kv-test").toFile();
        dir.deleteOnExit();
        System.setProperty("qingzhou.instance", dir.getAbsolutePath()); // 文件实现数据目录
    }

    @Test
    public void fileCrud() {
        testCrud(new FileKeyValueStore());
    }

    @Test
    public void memoryCrud() {
        testCrud(new MemoryKeyValueStore());
    }

    @Test
    public void fileUpdate() {
        testUpdate(new FileKeyValueStore());
    }

    @Test
    public void memoryUpdate() {
        testUpdate(new MemoryKeyValueStore());
    }

    @Test
    public void fileConcurrentUpdate() throws Exception {
        testConcurrentUpdate(new FileKeyValueStore());
    }

    @Test
    public void memoryConcurrentUpdate() throws Exception {
        testConcurrentUpdate(new MemoryKeyValueStore());
    }

    @Test
    public void contractValidation() {
        for (KeyValueStore store : new KeyValueStore[]{new FileKeyValueStore(), new MemoryKeyValueStore()}) {
            assertInvalid(() -> store.get("Bad_NS", "k1"));     // namespace 大写/下划线非法
            assertInvalid(() -> store.get("ns", "../escape"));  // key 路径危险字符
            assertInvalid(() -> store.get("ns", null));
            assertInvalid(() -> store.put("ns", "k1", null));   // value 必填
            char[] oversized = new char[KeyValueStore.MAX_VALUE_BYTES + 1];
            Arrays.fill(oversized, 'x');
            assertInvalid(() -> store.put("ns", "k1", new String(oversized))); // 超单值上限
            assertInvalid(() -> store.update("ns", "k1", null)); // fn 必填
        }
    }

    @Test
    public void contractBoundary() {
        KeyValueStore store = new MemoryKeyValueStore();
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < 128; i++) key.append('a');
        store.put("ns0", key.toString(), "v"); // 128 字符为 key 合法上限
        assertTrue(store.exists("ns0", key.toString()));
    }

    private void testCrud(KeyValueStore store) {
        assertFalse(store.exists("ns", "k1"));
        assertNull(store.get("ns", "k1"));
        assertFalse(store.delete("ns", "k1")); // 幂等：键不存在删除返回 false

        store.put("ns", "k1", "v1");
        assertTrue(store.exists("ns", "k1"));
        assertEquals(store.get("ns", "k1"), "v1");

        store.put("ns", "k1", "v2"); // 覆盖
        assertEquals(store.get("ns", "k1"), "v2");

        assertTrue(store.delete("ns", "k1"));
        assertNull(store.get("ns", "k1"));
    }

    private void testUpdate(KeyValueStore store) {
        assertEquals(store.update("ns", "counter", current -> { // 键不存在：fn 收到 null
            assertNull(current);
            return "1";
        }), "1"); // update 返回 fn 的结果
        assertEquals(store.get("ns", "counter"), "1");

        store.update("ns", "counter", current -> current + "0"); // 原子读改写
        assertEquals(store.get("ns", "counter"), "10");

        assertNull(store.update("ns", "counter", current -> null)); // 返回 null = 删除
        assertFalse(store.exists("ns", "counter"));
    }

    private void testConcurrentUpdate(KeyValueStore store) throws Exception {
        int threads = 8, loops = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger failed = new AtomicInteger();
        for (int i = 0; i < threads; i++) {
            pool.execute(() -> {
                try {
                    start.await();
                    for (int j = 0; j < loops; j++) {
                        store.update("ns", "race", current ->
                                current == null ? "1" : String.valueOf(Integer.parseInt(current) + 1));
                    }
                } catch (Exception e) {
                    failed.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS));
        pool.shutdownNow();
        assertEquals(failed.get(), 0, "并发 update 不得抛错丢更新");
        assertEquals(Integer.parseInt(store.get("ns", "race")), threads * loops, "同键互斥下读改写必须收敛");
    }

    private void assertInvalid(Runnable action) {
        try {
            action.run();
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // 契约要求：非法输入一律拒绝
        }
    }
}
