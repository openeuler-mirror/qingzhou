package qingzhou.agent.embedded.driver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceContainerProvider {
    private static final Map<Class<?>, Object> SERVICES = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<String, Object>> NAMED_SERVICES = new ConcurrentHashMap<>();

    public static <T> void registerService(Class<T> clazz, T instance) {
        SERVICES.put(clazz, instance);
    }

    public static <T> void registerService(Class<T> clazz, String name, T instance) {
        NAMED_SERVICES.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>()).put(name, instance);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getService(Class<T> clazz) {
        return (T) SERVICES.get(clazz);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getService(Class<T> clazz, String name) {
        Map<String, Object> named = NAMED_SERVICES.get(clazz);
        return named != null ? (T) named.get(name) : null;
    }

    public static void clear() {
        SERVICES.clear();
        NAMED_SERVICES.clear();
    }
}