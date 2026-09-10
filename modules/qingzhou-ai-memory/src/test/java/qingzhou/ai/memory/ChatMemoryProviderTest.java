package qingzhou.ai.memory;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import qingzhou.json.Json;
import qingzhou.llm.ChatMemory;
import qingzhou.logger.Logger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * 记忆装配组件的选择逻辑测试（脱离 DS 直接调用静态工厂）。
 */
public class ChatMemoryProviderTest {

    @BeforeClass
    public void setUp() throws Exception {
        // FileChatMemory 构造器读取实例目录，测试环境指向临时目录
        if (System.getProperty("qingzhou.instance") == null) {
            System.setProperty("qingzhou.instance", Files.createTempDirectory("qingzhou-test").toString());
        }
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
    public void missingOrEmptyTypeDisablesQuietly() {
        // 未配置 = 不启用记忆：安静降级为 null，不抛异常
        assertNull(ChatMemoryProvider.createDelegate(null, stub(Json.class), stub(Logger.class)));
        assertNull(ChatMemoryProvider.createDelegate(new HashMap<>(), stub(Json.class), stub(Logger.class)));
    }

    @Test
    public void typeMemorySelectsInMemory() {
        Map<String, String> config = new HashMap<>();
        config.put("type", "memory");
        ChatMemory memory = ChatMemoryProvider.createDelegate(config, stub(Json.class), stub(Logger.class));
        assertTrue(memory instanceof InMemoryChatMemory);
        // 实现类同时承载治理接口，Provider 才能以双服务注册
        assertTrue(memory instanceof ChatMemoryAdmin);

        // 行为验证：合法会话 id 被原样采信
        assertEquals(memory.resolveConversationId("user1", "conv-12345678"), "conv-12345678");
    }

    @Test
    public void typeFileSelectsFile() {
        Map<String, String> config = new HashMap<>();
        config.put("type", " file "); // 容忍空白
        assertTrue(ChatMemoryProvider.createDelegate(config, stub(Json.class), stub(Logger.class))
                instanceof FileChatMemory);
    }

    @Test
    public void unknownTypeDisablesQuietly() {
        // 非法值不抛异常：记 warn 后降级为“不启用记忆”
        Map<String, String> config = new HashMap<>();
        config.put("type", "redis");
        assertNull(ChatMemoryProvider.createDelegate(config, stub(Json.class), stub(Logger.class)));
    }
}
