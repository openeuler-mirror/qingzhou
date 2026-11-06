package qingzhou.ai.mcp.client;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import qingzhou.ai.SkillService;
import qingzhou.crypto.Crypto;
import qingzhou.http.client.HttpClient;
import qingzhou.json.Json;
import qingzhou.logger.Logger;

/**
 * 按 mcp.json 动态注册技能：每个 MCP Server 注册为一个 SkillService。
 */
@Component(configurationPid = "qingzhou-ai", configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true)
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

    @Activate
    public void init(ComponentContext context, Map<String, String> config) throws Exception {
        BundleContext bundleContext = context.getBundleContext();
        String qzVersion = config.get("qingzhou.version");
        Path mcpFile = Paths.get(config.get("qingzhou.instance"), "conf", "mcp.json");
        for (McpConfig server : McpConfig.load(mcpFile, json, crypto)) {
            McpClient client = new McpClient(server, httpClient, json, logger, qzVersion);
            McpSkillService skill = new McpSkillService(server, client, logger);

            Hashtable<String, Object> properties = new Hashtable<>();
            properties.put(SkillService.SKILL_NAME, server.id);
            properties.put(SkillService.SKILL_DESCRIPTION, description(server));
            properties.put(SkillService.SKILL_REQUIRED, Boolean.FALSE.toString()); // 按需匹配激活；McpServer 只绑定 true 的技能，故不对外暴露
            registrations.add(bundleContext.registerService(SkillService.class, skill, properties));
        }
    }

    private static String description(McpConfig server) {
        String desc = server.description;
        return desc == null || desc.trim().isEmpty()
                ? "由 MCP 服务器 " + server.id + " 提供的工具集。" : desc;
    }

    @Deactivate
    public void destroy() {
        registrations.forEach(ServiceRegistration::unregister);
    }
}
