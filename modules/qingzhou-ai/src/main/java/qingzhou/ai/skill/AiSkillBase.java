package qingzhou.ai.skill;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import qingzhou.ai.AiSkill;
import qingzhou.ai.AiTool;

abstract class AiSkillBase implements AiSkill {
    private final String[] nameI18n;
    private final String description;

    protected final Map<AiTool, Map<String, Object>> aiTools = new ConcurrentHashMap<>();

    protected AiSkillBase(String[] nameI18n, String description) {
        this.nameI18n = nameI18n;
        this.description = description;
    }

    @Override
    public String[] getI18n() {
        return nameI18n;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public Map<AiTool, Map<String, Object>> getTools() {
        return aiTools;
    }
}
