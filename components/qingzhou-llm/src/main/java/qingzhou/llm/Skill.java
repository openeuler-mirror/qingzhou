package qingzhou.llm;

import java.util.Collection;

public interface Skill {
    String name();

    String description();

    // 技能的说明书：激活后注入系统提示词，可用于引导 AI 如何使用该技能下的工具，如果没有工具，那就只是一段提示词增强
    String getInstruction();

    // 技能的工具集：该技能需要挂载的功能工具
    Collection<Tool> getTools();
}
