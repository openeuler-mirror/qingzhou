package qingzhou.ai.mcp.client;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import qingzhou.ai.SkillService;
import qingzhou.crypto.Crypto;
import qingzhou.http.client.HttpClient;
import qingzhou.json.Json;
import qingzhou.logger.Logger;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * 按 mcp.json 动态注册技能：每个 type=http 的 MCP Server 注册为一个 SkillService。
 */
@Component(immediate = true, configurationPid = "qingzhou-mcp", configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class McpSkillRegistry {
    private static final String CACHE_SECONDS = "cache_seconds";
    private static final long DEFAULT_CACHE_SECONDS = 3600;

    @Reference
    private HttpClient httpClient;
    @Reference
    private Json json;
    @Reference
    private Logger logger;
    @Reference
    private Crypto crypto;

    private final List<ServiceRegistration<?>> registrations = new ArrayList<>();
    private final List<McpSkillService> skills = new ArrayList<>();

    @Activate
    public void init(ComponentContext context, Map<String, ?> config) {
        long cacheMillis = cacheSeconds(config) * 1000;
        BundleContext bundleContext = context.getBundleContext();

        for (McpConfig server : McpConfig.load(json, crypto, logger)) {
            McpClient client = new McpClient(server, httpClient, json, logger);
            McpSkillService skill = new McpSkillService(server, client, logger, cacheMillis);
            skills.add(skill);

            Hashtable<String, Object> properties = new Hashtable<>();
            properties.put(SkillService.SKILL_NAME, server.id);
            properties.put(SkillService.SKILL_DESCRIPTION, description(server, client));
            properties.put(SkillService.SKILL_REQUIRED, "false"); // 按需匹配激活；McpServer 只绑定 true 的技能，故不对外暴露
            registrations.add(bundleContext.registerService(SkillService.class, skill, properties));
        }
    }

    private static String description(McpConfig server, McpClient client) {
        String desc;
        if (server.description != null && !server.description.trim().isEmpty()) {
            desc = server.description;
        } else {
            desc = client.getServerDescription();
        }
        if (desc != null) {
            return desc;
        }
        return "由 MCP 服务器 " + server.id + " 提供的工具集，工具名以 mcp__" + server.id + "__ 开头";
    }

    @Deactivate
    public void destroy() {
        for (ServiceRegistration<?> registration : registrations) {
            registration.unregister();
        }
        registrations.clear();
        skills.clear();
    }

    private long cacheSeconds(Map<String, ?> config) {
        if (config == null) return DEFAULT_CACHE_SECONDS;

        Object value = config.get(CACHE_SECONDS);
        if (value == null) return DEFAULT_CACHE_SECONDS;

        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            logger.warn("invalid " + CACHE_SECONDS + " [" + value + "], using default " + DEFAULT_CACHE_SECONDS);
            return DEFAULT_CACHE_SECONDS;
        }
    }
}
