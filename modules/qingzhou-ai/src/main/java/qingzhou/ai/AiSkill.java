package qingzhou.ai;

import java.util.Map;

public interface AiSkill {
    String SKILL_NAME = "SKILL_NAME";
    String SKILL_DESCRIPTION = "SKILL_DESCRIPTION";

    // 准入检查：依据当前对话上下文，决定是否需要激活该技能
    boolean isSupported(SkillContext chatContext);

    // 技能的说明书：激活后注入系统提示词，可用于引导 AI 如何使用该技能下的工具，如果没有工具，那就只是一段提示词增强
    String getInstruction();
    
    // 技能的工具集：该技能需要挂载的功能工具
    Map<AiTool, Map<String, Object>> getTools();
}
