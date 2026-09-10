package qingzhou.config.impl;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Dictionary;
import java.util.Map;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/** 测试共用辅助：动态代理桩、DS 字段注入、临时实例目录。 */
final class TestSupport {
    private TestSupport() {
    }

    @SuppressWarnings("unchecked")
    static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(TestSupport.class.getClassLoader(), new Class<?>[]{type}, handler);
    }

    /** 记录 getConfiguration / getFactoryConfiguration 得到的 pid 与 update 写入的属性字典。 */
    static ConfigurationAdmin admin(Map<String, Dictionary<String, Object>> updated) {
        return proxy(ConfigurationAdmin.class, (proxy, method, args) -> {
            if ("getConfiguration".equals(method.getName())) return configuration((String) args[0], updated);
            if ("getFactoryConfiguration".equals(method.getName())) {
                return configuration(args[0] + "~" + args[1], updated);
            }
            return defaultValue(method.getReturnType());
        });
    }

    /** 写入临时实例目录的 conf/qingzhou.properties，并指定 qingzhou.instance。 */
    static void instance(String content) throws Exception {
        Path dir = Files.createTempDirectory("qingzhou-instance");
        Path conf = Files.createDirectories(dir.resolve("conf"));
        Files.write(conf.resolve("qingzhou.properties"), content.getBytes(StandardCharsets.UTF_8));
        System.setProperty("qingzhou.instance", dir.toString());
    }

    /** 反射注入 DS 引用字段（测试不启动 OSGi 容器）。 */
    static void inject(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Object defaultValue(Class<?> type) {
        return type == boolean.class ? false : type == int.class ? 0 : null;
    }

    @SuppressWarnings("unchecked")
    private static Configuration configuration(String pid, Map<String, Dictionary<String, Object>> updated) {
        return proxy(Configuration.class, (proxy, method, args) -> {
            if ("update".equals(method.getName())) updated.put(pid, (Dictionary<String, Object>) args[0]);
            return defaultValue(method.getReturnType());
        });
    }
}
