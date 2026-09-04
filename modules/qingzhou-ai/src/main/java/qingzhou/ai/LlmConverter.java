package qingzhou.ai;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.service.component.ComponentConstants;
import qingzhou.llm.Parameter;
import qingzhou.llm.Skill;
import qingzhou.llm.Tool;

public class LlmConverter {
    public static Collection<Tool> convertAiTool(Map<ToolService, Map<String, Object>> aiTools) {
        return aiTools.entrySet().stream().map(entry -> convertTool(entry.getKey(), entry.getValue())).collect(Collectors.toSet());
    }

    public static Collection<Skill> convertAiSkill(Map<SkillService, Map<String, Object>> aiSkills) {
        return aiSkills.entrySet().stream().map(entry -> convertSkill(entry.getKey(), entry.getValue())).collect(Collectors.toSet());
    }

    public static Skill convertSkill(SkillService skillService, Map<String, Object> skillProp) {
        String skillName = skillProp.get(SkillService.SKILL_NAME).toString();
        boolean required = false;
        Object requiredStr = skillProp.get(SkillService.SKILL_REQUIRED);
        if (requiredStr != null) {
            required = Boolean.parseBoolean(requiredStr.toString());
        }
        return Skill.of(skillName,
                skillService.description(),
                skillService.instruction(),
                LlmConverter.convertAiTool(skillService.tools()), required);
    }

    public static Tool convertTool(ToolService toolService, Map<String, Object> toolProp) {
        String toolDescription = toolProp.get(ToolService.TOOL_DESCRIPTION).toString();
        String toolName;
        Object toolNameObj = toolProp.get(ToolService.TOOL_NAME);
        if (toolNameObj != null) {
            toolName = (String) toolNameObj;
        } else {
            Object componentName = toolProp.get(ComponentConstants.COMPONENT_NAME);
            if (componentName == null) {
                throw new IllegalArgumentException("missing parameter [" + ToolService.TOOL_NAME + "] for: " + toolDescription);
            }
            String component = componentName.toString();
            int i = component.lastIndexOf(".");
            toolName = component.substring(i + 1);
        }

        return Tool.of(toolName, toolDescription, parameters(toolProp), toolArgs -> {
            try {
                return toolService.invoke(toolArgs);
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
                ToolService.PARAMETER_NAME, ToolService.PARAMETER_DESCRIPTION, ToolService.PARAMETER_REQUIRED).forEach(flag -> {
            if (key.startsWith(flag)) {
                String keyPrefix = "";
                int i = key.indexOf(".");
                if (i != -1) {
                    keyPrefix = key.substring(i);
                }
                Map<String, String> param = params.computeIfAbsent(keyPrefix, s -> new HashMap<>());
                param.put(flag, (String) value);
            }
        }));

        return params.values().stream()
                .map(map -> Parameter.of(
                        map.get(ToolService.PARAMETER_NAME),
                        map.get(ToolService.PARAMETER_DESCRIPTION),
                        Boolean.parseBoolean(map.getOrDefault(ToolService.PARAMETER_REQUIRED, "true"))))
                .toArray(Parameter[]::new);
    }
}
