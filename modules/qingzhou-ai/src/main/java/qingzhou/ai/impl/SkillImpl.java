package qingzhou.ai.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.service.component.ComponentConstants;
import qingzhou.ai.AiSkill;
import qingzhou.ai.AiTool;
import qingzhou.llm.ChatContext;
import qingzhou.llm.Parameter;
import qingzhou.llm.Skill;
import qingzhou.llm.Tool;

class SkillImpl implements Skill {
    private final AiSkill skill;
    private final Map<String, Object> properties;

    SkillImpl(AiSkill skill, Map<String, Object> properties) {
        this.skill = skill;
        this.properties = properties;
    }

    @Override
    public String name() {
        return (String) properties.get(AiSkill.SKILL_NAME);
    }

    @Override
    public String description() {
        return (String) properties.get(AiSkill.SKILL_DESCRIPTION);
    }

    @Override
    public boolean isSupported(ChatContext chatContext) {
        return skill.isSupported(chatContext::attributes);
    }

    @Override
    public String getInstruction() {
        return skill.getInstruction();
    }

    @Override
    public Collection<Tool> getTools() {
        return skill.getTools().entrySet().stream().map(entry -> {
            AiTool aiTool = entry.getKey();
            Map<String, Object> toolProp = entry.getValue();
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
        }).collect(Collectors.toSet());
    }

    private Parameter[] parameters(Map<String, Object> toolProp) {
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
