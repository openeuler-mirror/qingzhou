package qingzhou.ai.skill;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import qingzhou.ai.AiSkill;
import qingzhou.ai.AiTool;

abstract class SkillBase implements AiSkill {
    protected final Map<AiTool, Map<String, Object>> aiTools = new ConcurrentHashMap<>();

    @Override
    public Map<AiTool, Map<String, Object>> getTools() {
        return aiTools;
    }
}
