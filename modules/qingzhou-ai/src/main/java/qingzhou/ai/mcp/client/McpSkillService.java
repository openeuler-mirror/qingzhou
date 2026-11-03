package qingzhou.ai.mcp.client;

import java.util.*;
import java.util.regex.Pattern;

import qingzhou.ai.SkillService;
import qingzhou.ai.ToolService;
import qingzhou.logger.Logger;

final class McpSkillService implements SkillService {
    private static final int MAX_NAME_LENGTH = 64; // OpenAI function name 约束
    private static final Pattern ILLEGAL_CHAR = Pattern.compile("[^a-zA-Z0-9_-]");

    private final McpConfig config;
    private final McpClient client;
    private final Logger logger;
    private volatile Map<ToolService, Map<String, Object>> cached;

    McpSkillService(McpConfig config, McpClient client, Logger logger) {
        this.config = config;
        this.client = client;
        this.logger = logger;
    }

    @Override
    public Map<ToolService, Map<String, Object>> tools() {
        if (cached == null) {
            synchronized (this) {
                if (cached == null) {
                    Map<ToolService, Map<String, Object>> declared = new HashMap<>();
                    try {
                        for (Map<String, Object> tool : client.listTools()) {
                            String name = Objects.toString(tool.get("name"), "");
                            if (!name.isEmpty())
                                declared.put(args -> client.callTool(name, args), properties(name, tool));
                        }
                    } catch (Exception e) {
                        logger.warn("failed to load tools of mcp server [" + config.id + "]: " + e.getMessage());
                    }
                    cached = declared;
                }
            }
        }
        return cached;
    }

    // mcp__{server}__{tool} 三段式命名，避免不同 Server 的同名工具互相覆盖
    private Map<String, Object> properties(String toolName, Map<String, Object> tool) {
        String name = ILLEGAL_CHAR.matcher("mcp__" + config.id + "__" + toolName).replaceAll("_");
        String exposed = name.substring(0, Math.min(name.length(), MAX_NAME_LENGTH));

        Map<String, Object> declared = new LinkedHashMap<>();
        declared.put(ToolService.TOOL_NAME, exposed);
        String description = Objects.toString(tool.get("description"), "");
        declared.put(ToolService.TOOL_DESCRIPTION, description.isEmpty() ? exposed : description);

        Object schemaNode = tool.get("inputSchema");
        Map<?, ?> schema = schemaNode instanceof Map ? (Map<?, ?>) schemaNode : Collections.emptyMap();
        Object propertiesNode = schema.get("properties");
        if (!(propertiesNode instanceof Map)) return declared;

        Object requiredNode = schema.get("required");
        List<?> required = requiredNode instanceof List ? (List<?>) requiredNode : Collections.emptyList();
        int index = 0;
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) propertiesNode).entrySet()) {
            Map<?, ?> param = (Map<?, ?>) entry.getValue();
            String suffix = "." + ++index;
            declared.put(ToolService.PARAMETER_NAME + suffix, entry.getKey());
            declared.put(ToolService.PARAMETER_DESCRIPTION + suffix, Objects.toString(param.get("description"), ""));
            declared.put(ToolService.PARAMETER_REQUIRED + suffix, Boolean.toString(required.contains(entry.getKey())));
            declared.put(ToolService.PARAMETER_TYPE + suffix, Objects.toString(param.get("type"), ""));
        }
        return declared;
    }
}
