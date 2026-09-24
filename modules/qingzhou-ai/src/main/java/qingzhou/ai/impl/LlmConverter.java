package qingzhou.ai.impl;

import java.util.*;
import java.util.stream.Stream;

import org.osgi.service.component.ComponentConstants;
import qingzhou.ai.SkillService;
import qingzhou.ai.ToolService;
import qingzhou.llm.Parameter;
import qingzhou.llm.Skill;
import qingzhou.llm.Tool;
import qingzhou.logger.Logger;

public final class LlmConverter {
    /**
     * 把各模块声明的技能与工具转换为大模型可用的结构。
     * <p>
     * 技能与工具来自任意模块，声明不完整是常态而非异常：单个条目不合法时只跳过它本身并告警，
     * 不让一处笔误导致整个 AI 能力不可用。
     */
    public static Collection<Skill> convertSkills(Map<SkillService, Map<String, Object>> aiSkills, Logger logger) {
        Collection<Skill> skills = new HashSet<>();
        Map<String, String> toolOwners = new HashMap<>(); // 工具名 -> 声明它的类，用于跨模块重名检测
        for (Map.Entry<SkillService, Map<String, Object>> entry : aiSkills.entrySet()) {
            try {
                Skill skill = convertSkill(entry.getKey(), entry.getValue(), toolOwners, logger);
                if (skill != null) skills.add(skill);
            } catch (Throwable t) {
                logger.warn("skip invalid skill [" + typeName(entry.getKey()) + "]: " + t.getMessage());
            }
        }
        return skills;
    }

    private static Skill convertSkill(SkillService skillService, Map<String, Object> skillProp,
                                      Map<String, String> toolOwners, Logger logger) {
        String name = text(skillProp, SkillService.SKILL_NAME);
        String description = text(skillProp, SkillService.SKILL_DESCRIPTION);
        if (name == null || description == null) {
            logger.warn("skip skill [" + typeName(skillService) + "]: missing "
                    + (name == null ? SkillService.SKILL_NAME : SkillService.SKILL_DESCRIPTION));
            return null;
        }
        return Skill.of(name, description, skillService.instruction(),
                convertTools(skillService.tools(), toolOwners, logger),
                Boolean.parseBoolean(text(skillProp, SkillService.SKILL_REQUIRED)));
    }

    private static Collection<Tool> convertTools(Map<ToolService, Map<String, Object>> aiTools,
                                                 Map<String, String> toolOwners, Logger logger) {
        if (aiTools == null) return Collections.emptySet();

        Collection<Tool> tools = new HashSet<>();
        for (Map.Entry<ToolService, Map<String, Object>> entry : aiTools.entrySet()) {
            String toolTypeName = typeName(entry.getKey());
            try {
                Tool tool = convertTool(entry.getKey(), entry.getValue(), logger);
                if (tool == null) continue;

                String owner = toolOwners.putIfAbsent(tool.name(), toolTypeName);
                if (owner != null) { // 重名工具模型无法区分，保留先到者并告警
                    logger.warn("skip duplicate tool [" + tool.name() + "] of [" + toolTypeName
                            + "], already declared by [" + owner + "]");
                    continue;
                }
                tools.add(tool);
            } catch (Throwable t) {
                logger.warn("skip invalid tool [" + toolTypeName + "]: " + t.getMessage());
            }
        }
        return tools;
    }

    private static Tool convertTool(ToolService toolService, Map<String, Object> toolProp, Logger logger) {
        String name = toolName(toolProp);
        String description = text(toolProp, ToolService.TOOL_DESCRIPTION);
        if (name == null || description == null) {
            logger.warn("skip tool [" + typeName(toolService) + "]: missing "
                    + (name == null ? ToolService.TOOL_NAME + " or " + ComponentConstants.COMPONENT_NAME
                    : ToolService.TOOL_DESCRIPTION));
            return null;
        }

        return Tool.of(name, description, parameters(toolProp), toolArgs -> {
            try {
                return toolService.invoke(toolArgs);
            } catch (Exception e) {
                throw new RuntimeException(
                        toolArgs != null ? toolArgs.toString() : e.getMessage(),
                        e);
            }
        });
    }

    // 未显式声明 TOOL_NAME 时回退到组件简单名，两者都取不到则视为声明不完整
    private static String toolName(Map<String, Object> toolProp) {
        Object declared = toolProp.get(ToolService.TOOL_NAME);
        if (declared != null) return (String) declared;

        Object component = toolProp.get(ComponentConstants.COMPONENT_NAME);
        if (component == null) return null;
        String name = component.toString();
        int i = name.lastIndexOf('.');
        return i < 0 ? name : name.substring(i + 1);
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

    private static String text(Map<String, Object> props, String key) {
        Object value = props.get(key);
        return value == null ? null : value.toString();
    }

    // lambda 形式的工具会带上宿主类名，足以定位声明方
    private static String typeName(Object service) {
        return service.getClass().getName();
    }
}
