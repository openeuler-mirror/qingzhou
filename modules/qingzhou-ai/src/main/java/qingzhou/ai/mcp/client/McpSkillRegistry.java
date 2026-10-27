package qingzhou.ai.mcp.client;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import qingzhou.ai.SkillService;
import qingzhou.crypto.Crypto;
import qingzhou.http.client.HttpClient;
import qingzhou.json.Json;
import qingzhou.logger.Logger;

/**
 * 按 mcp.json 动态注册技能：每个 type=http 的 MCP Server 注册为一个 SkillService。
 */
@Component(immediate = true)
public class McpSkillRegistry {
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
    public void init(ComponentContext context) {
        BundleContext bundleContext = context.getBundleContext();

        for (McpConfig server : McpConfig.load(json, crypto, logger)) {
            McpClient client = new McpClient(server, httpClient, json, logger);
            McpSkillService skill = new McpSkillService(server, client, logger);
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
}
