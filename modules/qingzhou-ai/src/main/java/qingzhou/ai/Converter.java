package qingzhou.ai;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.service.component.ComponentConstants;
import qingzhou.llm.Parameter;
import qingzhou.llm.Tool;

public class Converter {
    public static Collection<Tool> convertSystemAiTool(Map<SystemAiTool, Map<String, Object>> aiTools) {
        return aiTools.entrySet().stream().map(entry -> convertTool(entry.getKey(), entry.getValue())).collect(Collectors.toSet());
    }

    public static Collection<Tool> convertAiTool(Map<AiTool, Map<String, Object>> aiTools) {
        return aiTools.entrySet().stream().map(entry -> convertTool(entry.getKey(), entry.getValue())).collect(Collectors.toSet());
    }

    private static Tool convertTool(AiTool aiTool, Map<String, Object> toolProp) {
        String toolDescription = toolProp.get(AiTool.TOOL_DESCRIPTION).toString();
        String toolName;
        Object toolNameObj = toolProp.get(AiTool.TOOL_NAME);
        if (toolNameObj != null) {
            toolName = (String) toolNameObj;
        } else {
            Object componentName = toolProp.get(ComponentConstants.COMPONENT_NAME);
            if (componentName == null) {
                throw new IllegalArgumentException("missing parameter [" + AiTool.TOOL_NAME + "] for: " + toolDescription);
            }
            String component = componentName.toString();
            int i = component.lastIndexOf(".");
            toolName = component.substring(i + 1);
        }

        return Tool.of(toolName, toolDescription, parameters(toolProp), toolArgs -> {
            try {
                return aiTool.invoke(toolArgs);
            } catch (Exception e) {
                throw new RuntimeException(
                        toolArgs != null ? toolArgs.toString() : e.getMessage(),
                        e);
            }
        });
    }

    private static Parameter[] parameters(Map<String, Object> toolProp) {
        Map<String, Map<String, String>> params = new LinkedHashMap<>();

        toolProp.forEach((key, value) -> Stream.of(
                AiTool.PARAMETER_NAME, AiTool.PARAMETER_DESCRIPTION, AiTool.PARAMETER_REQUIRED).forEach(flag -> {
            if (key.startsWith(flag)) {
                String sp = "";
                int i = key.indexOf(".");
                if (i != -1) {
                    sp = key.substring(i);
                }
                Map<String, String> param = params.computeIfAbsent(sp, s -> new HashMap<>());
                param.put(flag, (String) value);
            }
        }));

        return params.values().stream()
                .map(map -> Parameter.of(
                        map.get(AiTool.PARAMETER_NAME),
                        map.get(AiTool.PARAMETER_DESCRIPTION),
                        Boolean.parseBoolean(map.getOrDefault(AiTool.PARAMETER_REQUIRED, "true"))))
                .toArray(Parameter[]::new);
    }
}
