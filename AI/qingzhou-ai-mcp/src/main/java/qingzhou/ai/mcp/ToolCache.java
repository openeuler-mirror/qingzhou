package qingzhou.ai.mcp;

import java.util.*;

import qingzhou.ai.ToolParameterParser;
import qingzhou.ai.ToolService;

class ToolCache {
    final String toolName;
    final String toolDescription;
    final Map<String, Object> toolSchema;
    final ToolService toolService;

    ToolCache(ToolService toolService, Map<String, Object> properties, ToolParameterParser parameterParser) {
        Object name = properties.get(ToolService.TOOL_NAME);
        if (!(name instanceof String) || ((String) name).isEmpty()) {
            throw new IllegalArgumentException("missing " + ToolService.TOOL_NAME + " of " + toolService.getClass().getName());
        }
        this.toolName = (String) name;
        this.toolDescription = Objects.toString(properties.get(ToolService.TOOL_DESCRIPTION), null);
        this.toolService = toolService;

        Collection<ToolParameterParser.ToolParameter> toolParameters = parameterParser.parse(properties);
        this.toolSchema = buildSchema(toolParameters);
    }

    private Map<String, Object> buildSchema(Collection<ToolParameterParser.ToolParameter> toolParameters) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("name", toolName);
        if (toolDescription != null) { // schema description 须为 string，缺省省略
            schema.put("description", toolDescription);
        }

        List<String> requiredParameters = new ArrayList<>();
        // properties 契约为 JSON Schema object（参数名 → 参数 schema）
        Map<String, Object> parameterProperties = new HashMap<>();
        for (ToolParameterParser.ToolParameter parameter : toolParameters) {
            if (parameter.name == null || parameter.name.isEmpty()) {
                throw new IllegalArgumentException("missing " + ToolService.PARAMETER_NAME + " of tool " + toolName);
            }
            Map<String, Object> parameterSchema = new HashMap<>();
            // type 缺省为 string：JSON Schema 的 type 不接受 null
            parameterSchema.put("type", parameter.type == null || parameter.type.isEmpty() ? "string" : parameter.type);
            if (parameter.description != null) {
                parameterSchema.put("description", parameter.description);
            }
            parameterProperties.put(parameter.name, parameterSchema);

            if (parameter.required) {
                requiredParameters.add(parameter.name);
            }
        }

        Map<String, Object> inputSchema = new HashMap<>();
        inputSchema.put("type", "object");
        inputSchema.put("properties", parameterProperties);
        if (!requiredParameters.isEmpty()) { // 空 required 对旧 JSON Schema 校验器非法，缺省省略
            inputSchema.put("required", requiredParameters);
        }
        schema.put("inputSchema", inputSchema);
        return schema;
    }
}
