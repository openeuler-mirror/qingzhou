package qingzhou.llm.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import org.noear.solon.ai.chat.skill.SkillDesc;
import org.noear.solon.ai.chat.skill.SkillProvider;
import org.noear.solon.ai.chat.tool.FunctionTool;
import org.noear.solon.ai.chat.tool.FunctionToolDesc;
import org.noear.solon.ai.chat.tool.ToolProvider;
import qingzhou.llm.Parameter;
import qingzhou.llm.Skill;
import qingzhou.llm.Tool;

class Converter {
    static ToolProvider convertTool(Collection<Tool> tools) {
        return () -> {
            if (tools == null) return Collections.emptySet();
            return tools.stream().map(Converter::convertTool).collect(Collectors.toSet());
        };
    }

    static SkillProvider convertSkill(Collection<Skill> skills) {
        return () -> {
            if (skills == null) return Collections.emptySet();
            return skills.stream().map(Converter::convertSkill).collect(Collectors.toSet());
        };
    }

    private static org.noear.solon.ai.chat.skill.Skill convertSkill(Skill skill) {
        return SkillDesc.builder(skill.name())
                .description(skill.description())
                .instruction(skill.getInstruction())
                .toolAdd(() -> {
                    Collection<Tool> tools = skill.getTools();
                    if (tools == null) return Collections.emptyList();
                    return tools.stream().map(Converter::convertTool).collect(Collectors.toSet());
                }).build();
    }

    private static FunctionTool convertTool(Tool tool) {
        FunctionToolDesc functionTool = new FunctionToolDesc(tool.name())
                .description(tool.description())
                .doHandle(tool::invoke);

        Parameter[] parameters = tool.parameters();
        if (parameters != null) {
            for (Parameter parameter : parameters) {
                functionTool.paramAdd(parameter.name(), String.class, parameter.required(), parameter.description(), null, null);
                String[] list = parameter.enumValues();
                if (list != null && list.length != 0) {
                    String enumValues = Arrays.toString(list);
                    functionTool.description(functionTool.description() + " Optional values: " + enumValues + ", you can only choose one of these values as input.");
                }
            }
        }
        return functionTool;
    }
}
