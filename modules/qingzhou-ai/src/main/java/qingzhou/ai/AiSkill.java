package qingzhou.ai;

import java.util.Map;

public interface AiSkill {
    String SKILL_NAME = "SKILL_NAME";
    String SKILL_DESCRIPTION = "SKILL_DESCRIPTION";

    // 准入检查：依据当前对话上下文，决定是否需要激活该技能
    boolean isSupported(SkillContext chatContext);

    // 技能的工具集：该技能需要挂载的功能工具
    Map<AiTool, Map<String, Object>> getTools();
}
