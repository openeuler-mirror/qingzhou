package qingzhou.ai.memory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import qingzhou.json.impl.JsonImpl;
import qingzhou.logger.Logger;
import qingzhou.store.impl.StoreFactoryImpl;

/**
 * 会话记忆用例的公共夹具：不起 OSGI 容器，反射注入依赖后直接调用 init
 */
final class MemoryTestSupport {
    private static final List<ConversationStore> CREATED = new ArrayList<>();

    /**
     * 配额类用例的基础配置：压缩阈值放大，只覆盖同步的配额与裁剪逻辑
     */
    static Map<String, String> config(int maxPerUser) {
        Map<String, String> config = new HashMap<>();
        config.put("memory_store", "memory");
        config.put("memory_max_conversations_per_user", String.valueOf(maxPerUser));
        config.put("memory_max_conversations_anonymous", String.valueOf(maxPerUser));
        config.put("conversation_soft_messages", "1000");
        config.put("conversation_hard_messages", "1000");
        config.put("conversation_keep_messages", "1000");
        return config;
    }

    static ConversationStore newStore(Map<String, String> config) throws Exception {
        ConversationStore store = new ConversationStore();
        JsonImpl json = new JsonImpl();
        json.init(); // @Activate
        setField(store, "json", json);
        setField(store, "logger", stub(Logger.class));
        setField(store, "storeFactory", new StoreFactoryImpl());
        setField(store, "chatModelFactory", null); // 未配置模型：摘要走拼接式回退
        store.init(config);
        CREATED.add(store);
        return store;
    }

    /**
     * 关闭用例创建的存储：回收压缩线程，避免用例间残留后台线程
     */
    static void shutdownAll() {
        for (ConversationStore store : CREATED) store.destroy();
        CREATED.clear();
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
