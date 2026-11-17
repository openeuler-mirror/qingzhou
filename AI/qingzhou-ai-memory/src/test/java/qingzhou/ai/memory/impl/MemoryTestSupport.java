package qingzhou.ai.memory.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.testng.Assert;
import qingzhou.json.impl.JsonImpl;
import qingzhou.logger.Logger;
import qingzhou.store.StoreFactory;
import qingzhou.store.impl.StoreFactoryImpl;

/**
 * 会话记忆用例的公共夹具：不起 OSGI 容器，反射注入依赖后直接调用 init
 */
final class MemoryTestSupport {
    private static final long AWAIT_TIMEOUT_MS = 5000L;
    private static final List<ConversationStoreImpl> CREATED = new ArrayList<>();

    /**
     * 配额类用例的基础配置：压缩阈值放大，只覆盖同步的配额与裁剪逻辑
     */
    static Map<String, String> config(int maxPerUser) {
        Map<String, String> config = new HashMap<>();
        config.put("memory_store", "memory");
        config.put("memory_max_conversations_per_user", String.valueOf(maxPerUser));
        config.put("memory_max_conversations_anonymous", String.valueOf(maxPerUser));
        config.put("conversation_soft_messages", "1000");
        config.put("conversation_hard_messages", "2000");
        config.put("conversation_keep_messages", "400");
        return config;
    }

    static ConversationStoreImpl newStore(Map<String, String> config) throws Exception {
        return newStore(config, new StoreFactoryImpl());
    }

    static ConversationStoreImpl newStore(Map<String, String> config, StoreFactory storeFactory) throws Exception {
        ConversationStoreImpl store = new ConversationStoreImpl();
        JsonImpl json = new JsonImpl();
        json.init(); // @Activate
        setField(store, "json", json);
        setField(store, "logger", stub(Logger.class));
        setField(store, "storeFactory", storeFactory);
        setField(store, "chatModelFactory", null); // 未配置模型：摘要走拼接式回退
        store.init(config);
        CREATED.add(store);
        return store;
    }

    /**
     * 关闭用例创建的存储：回收压缩线程，避免用例间残留后台线程
     */
    static void shutdownAll() {
        for (ConversationStoreImpl store : CREATED) store.destroy();
        CREATED.clear();
    }

    /**
     * 有界等待异步收敛：压缩为后台任务，无回调可取，只能轮询公开视图
     */
    static void awaitUntil(BooleanSupplier condition, String message) throws InterruptedException {
        long deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) return;
            Thread.sleep(10);
        }
        Assert.fail(message);
    }

    static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    static String repeat(String text, int times) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < times; i++) builder.append(text);
        return builder.toString();
    }

    static <T> T stub(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(
                MemoryTestSupport.class.getClassLoader(),
                new Class<?>[]{type},
                MemoryTestSupport::defaultValue));
    }

    private static Object defaultValue(Object proxy, Method method, Object[] args) {
        String name = method.getName();
        if (name.startsWith("is") && method.getReturnType() == boolean.class) return false;
        if ("hashCode".equals(name)) return System.identityHashCode(proxy);
        if ("equals".equals(name)) return args != null && args.length > 0 && proxy == args[0];
        return null;
    }
}
