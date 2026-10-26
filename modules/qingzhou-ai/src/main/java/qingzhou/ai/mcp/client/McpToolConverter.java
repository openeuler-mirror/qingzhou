package qingzhou.ai.mcp.client;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import qingzhou.ai.ToolService;

final class McpToolConverter {
    private static final String NAME_PREFIX = "mcp__";
    private static final String NAME_SEPARATOR = "__";
    private static final int MAX_NAME_LENGTH = 64; // OpenAI function name 约束
    private static final int MAX_RESULT_CHARS = 20_000;
    private static final Pattern ILLEGAL_CHAR = Pattern.compile("[^a-zA-Z0-9_-]");

    private McpToolConverter() {
    }

    /**
     * 采用 mcp__{server}__{tool} 三段式命名，避免不同 Server 的同名工具互相覆盖。
     */
    static String exposedName(String serverId, String toolName) {
        String name = NAME_PREFIX + normalize(serverId) + NAME_SEPARATOR + normalize(toolName);
        return name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name;
    }

    static Map<String, Object> properties(String exposedName, String description, Object inputSchema) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put(ToolService.TOOL_NAME, exposedName);
        properties.put(ToolService.TOOL_DESCRIPTION, isBlank(description) ? exposedName : description);

        Map<?, ?> schema = inputSchema instanceof Map ? (Map<?, ?>) inputSchema : Collections.emptyMap();
        List<String> required = requiredNames(schema.get("required"));

        Object propertiesNode = schema.get("properties");
        if (propertiesNode instanceof Map) {
            int index = 0;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) propertiesNode).entrySet()) {
                index++;
                String name = String.valueOf(entry.getKey());
                Map<String, ?> value = (Map<String, ?>) entry.getValue();
                properties.put(ToolService.PARAMETER_NAME + "." + index, name);
                properties.put(ToolService.PARAMETER_DESCRIPTION + "." + index, text(value.remove("description")));
                properties.put(ToolService.PARAMETER_REQUIRED + "." + index, Boolean.toString(required.contains(name)));
                properties.put(ToolService.PARAMETER_TYPE + "." + index, text(value.remove("type")));
            }
        }
        return properties;
    }

    static String extractText(Map<String, Object> result) throws IOException {
        Object content = result.get("content");
        if (!(content instanceof List)) {
            if (Boolean.TRUE.equals(result.get("isError"))) throw new IOException("mcp tool reported an error");
            return "";
        }

        StringBuilder text = new StringBuilder();
        for (Object item : (List<?>) content) {
            if (!(item instanceof Map)) continue;

            Map<?, ?> part = (Map<?, ?>) item;
            if (text.length() > 0) text.append('\n');
            if ("text".equals(part.get("type"))) {
                Object value = part.get("text");
                text.append(value == null ? "" : String.valueOf(value));
            } else {
                text.append('[').append(part.get("type")).append(" content]"); // 非文本内容无法以文本呈现，标注类型避免模型误判
            }
        }

        String value = text.toString();
        if (value.length() > MAX_RESULT_CHARS) {
            value = value.substring(0, MAX_RESULT_CHARS) + "\n…（内容过长已截断）";
        }
        if (Boolean.TRUE.equals(result.get("isError"))) {
            throw new IOException(value.isEmpty() ? "mcp tool reported an error" : value);
        }
        return value;
    }

    private static List<String> requiredNames(Object node) {
        List<String> required = new ArrayList<>();
        if (node instanceof List) {
            for (Object item : (List<?>) node) required.add(String.valueOf(item));
        }
        return required;
    }

    private static String description(Object definition) {
        if (!(definition instanceof Map)) return "";

        Object description = ((Map<?, ?>) definition).get("description");
        return description == null ? "" : String.valueOf(description);
    }

    private static String normalize(String raw) {
        return ILLEGAL_CHAR.matcher(raw == null ? "" : raw).replaceAll("_");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
