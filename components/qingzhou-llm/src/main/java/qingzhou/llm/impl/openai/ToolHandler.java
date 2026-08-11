package qingzhou.llm.impl.openai;

import java.util.*;

import qingzhou.json.Json;
import qingzhou.llm.*;

class ToolHandler {
    private final OpenAiChatModelBuilder builder;
    private final Json json;

    ToolHandler(OpenAiChatModelBuilder builder, Json json) {
        this.builder = builder;
        this.json = json;
    }

    List<Object> buildToolDefinitions() {
        List<Tool> allTools = new ArrayList<>();
        if (builder.tools != null) allTools.addAll(builder.tools);
        if (builder.skills != null) {
            for (Skill skill : builder.skills) {
                Collection<Tool> skillTools = skill.tools();
                if (skillTools != null) allTools.addAll(skillTools);
            }
        }

        if (allTools.isEmpty()) return Collections.emptyList();

        List<Object> toolDefs = new ArrayList<>();
        for (Tool tool : allTools) {
            Map<String, Object> toolDef = new HashMap<>();
            toolDef.put("type", "function");

            Map<String, Object> function = new HashMap<>();
            function.put("name", tool.name());
            function.put("description", tool.description());

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "object");

            Parameter[] params = tool.parameters();
            if (params != null && params.length > 0) {
                Map<String, Object> properties = new HashMap<>();
                List<String> required = new ArrayList<>();
                for (Parameter param : params) {
                    Map<String, Object> prop = new HashMap<>();
                    prop.put("type", "string");
                    prop.put("description", param.description());
                    properties.put(param.name(), prop);
                    if (param.required()) {
                        required.add(param.name());
                    }
                }
                parameters.put("properties", properties);
                if (!required.isEmpty()) {
                    parameters.put("required", required);
                }
            } else {
                parameters.put("properties", new HashMap<>());
            }

            function.put("parameters", parameters);
            toolDef.put("function", function);
            toolDefs.add(toolDef);
        }
        return toolDefs;
    }

    @SuppressWarnings("unchecked")
    void executeToolCall(Map<String, Object> toolCall, List<Object> messages, Listener listener) {
        Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
        String toolName = (String) function.get("name");
        String argumentsStr = (String) function.get("arguments");

        Map<String, Object> args = new HashMap<>();
        if (argumentsStr != null && !argumentsStr.isEmpty()) {
            try {
                args = json.fromJson(argumentsStr, Map.class);
            } catch (Exception ignored) {
                args = new HashMap<>();
            }
        }

        Tool tool = findTool(toolName);
        String result;
        if (tool != null) {
            try {
                result = tool.invoke(args);
            } catch (Throwable t) {
                result = "Error: " + t.getMessage();
            }
        } else {
            result = "Tool not found: " + toolName;
        }

        listener.onToolCall(toolName, args, result);

        Map<String, Object> toolResultMsg = new HashMap<>();
        toolResultMsg.put("role", "tool");
        toolResultMsg.put("tool_call_id", toolCall.get("id"));
        toolResultMsg.put("content", result);
        messages.add(toolResultMsg);
    }

    private Tool findTool(String name) {
        if (builder.tools != null) {
            for (Tool tool : builder.tools) {
                if (tool.name().equals(name)) return tool;
            }
        }
        if (builder.skills != null) {
            for (Skill skill : builder.skills) {
                Collection<Tool> skillTools = skill.tools();
                if (skillTools != null) {
                    for (Tool tool : skillTools) {
                        if (tool.name().equals(name)) return tool;
                    }
                }
            }
        }
        return null;
    }
}
