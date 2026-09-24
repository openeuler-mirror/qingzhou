package qingzhou.ai;

import java.util.Map;

public interface SkillService {
    String SKILL_NAME = "SKILL_NAME";
    String SKILL_DESCRIPTION = "SKILL_DESCRIPTION";
    String SKILL_REQUIRED = "SKILL_REQUIRED";

    // 技能的说明书：激活后注入系统提示词，可用于引导 AI 如何使用该技能下的工具，如果没有工具，那就只是一段提示词增强
    default String instruction() {
        return null;
    }

    // 技能的工具集：该技能需要挂载的功能工具
    default Map<ToolService, Map<String, Object>> tools() {
        return null;
    }
}
