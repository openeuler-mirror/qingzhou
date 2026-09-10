package qingzhou.ai.memory;

import java.util.Map;

import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import qingzhou.json.Json;
import qingzhou.llm.ChatMemory;
import qingzhou.logger.Logger;

/**
 * 对话记忆装配组件：按 {@code qingzhou.properties} 的 {@code qingzhou-ai-memory.type}
 * 选择实现（file={@link FileChatMemory}，memory={@link InMemoryChatMemory}）；
 * 未配置、配置为空或取值非法时不注册任何服务（非法值仅记 warn 日志），对话退化为单轮无记忆。
 */
@Component(immediate = true,
        configurationPid = "qingzhou-ai-memory",
        configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class ChatMemoryProvider {
    public static final String TYPE_FILE = "file";
    public static final String TYPE_MEMORY = "memory";

    @Reference
    private Json json;
    @Reference
    private Logger logger;

    /** 显式配置了有效 type 才有值；手动注册以便未启用时不出现在服务注册表 */
    private ServiceRegistration<ChatMemory> memoryRegistration;
    private ServiceRegistration<ChatMemoryAdmin> adminRegistration;

    @Activate
    public void init(ComponentContext ctx, Map<String, String> config) {
        ChatMemory delegate = createDelegate(config, json, logger);
        if (delegate == null) return;
        // 同一实例注册两个服务：对话路径与治理路径各取所需接口
        memoryRegistration = ctx.getBundleContext().registerService(ChatMemory.class, delegate, null);
        adminRegistration = ctx.getBundleContext()
                .registerService(ChatMemoryAdmin.class, (ChatMemoryAdmin) delegate, null);
    }

    @Deactivate
    public void stop() {
        if (memoryRegistration != null) {
            memoryRegistration.unregister();
            memoryRegistration = null;
        }
        if (adminRegistration != null) {
            adminRegistration.unregister();
            adminRegistration = null;
        }
    }

    /** 返回 null 表示不启用记忆（未配置/配置为空/值非法，安静降级不抛异常） */
    static ChatMemory createDelegate(Map<String, String> config, Json json, Logger logger) {
        String type = config == null ? null : config.get("type");
        if (type == null || type.trim().isEmpty()) {
            return null;
        }
        type = type.trim();
        switch (type) {
            case TYPE_FILE:
                return new FileChatMemory(json, logger);
            case TYPE_MEMORY:
                return new InMemoryChatMemory();
            default:
                logger.warn("qingzhou-ai-memory.type=" + type + " is invalid, chat memory is disabled (expect "
                        + TYPE_FILE + " or " + TYPE_MEMORY + ")");
                return null;
        }
    }
}
