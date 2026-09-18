package qingzhou.store.impl;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

public class FileStoreTest {

    private FileStore newStore() throws Exception {
        return new FileStore(Files.createTempDirectory("qingzhou-store-").toFile());
    }

    @Test
    public void putThenGet_roundTrip_restoreValue() throws Exception {
        FileStore store = newStore();
        store.put("app-config", "value-1");
        Assert.assertEquals(store.get("app-config"), "value-1");
        Assert.assertTrue(store.contains("app-config"));
    }

    @Test
    public void overwritePut_get_returnLatestValue() throws Exception {
        FileStore store = newStore();
        store.put("key", "old-value");
        store.put("key", "new-value");
        Assert.assertEquals(store.get("key"), "new-value");
    }

    @Test
    public void missingKey_get_returnNull() throws Exception {
        FileStore store = newStore();
        Assert.assertNull(store.get("no-such-key"));
        Assert.assertFalse(store.contains("no-such-key"));
    }

    @Test
    public void deletedKey_get_returnNull() throws Exception {
        FileStore store = newStore();
        store.put("key", "value");
        store.delete("key");
        Assert.assertNull(store.get("key"));
        Assert.assertFalse(store.contains("key"));
    }

    @Test
    public void keys_afterPut_returnStoredKeys() throws Exception {
        FileStore store = newStore();
        store.put("key-a", "a");
        store.put("key-b", "b");
        Set<String> expected = new HashSet<>(Arrays.asList("key-a", "key-b"));
        Assert.assertEquals(store.keys(), expected);
    }

    @Test
    public void parentTraversalKey_put_throwIllegalArgument() throws Exception {
        FileStore store = newStore();
        try {
            store.put("../escape", "value");
            Assert.fail("路径穿越 key 应拒绝");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void absoluteKey_get_throwIllegalArgument() throws Exception {
        FileStore store = newStore();
        try {
            store.get("/etc/passwd");
            Assert.fail("绝对路径 key 应拒绝");
        } catch (IllegalArgumentException expected) {
        }
    }
}
