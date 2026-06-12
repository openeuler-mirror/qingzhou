package qingzhou.ai.skill;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import qingzhou.ai.AiSkill;
import qingzhou.ai.AiTool;
import qingzhou.ai.SkillContext;

abstract class SkillBase implements AiSkill {
    protected final Map<AiTool, Map<String, Object>> aiTools = new ConcurrentHashMap<>();

    @Override
    public boolean isSupported(SkillContext chatContext) {
        Map<String, Object> attributes = chatContext.attributes();
        // note： 待增加权限验证、审计等逻辑，同时参考 ToolInterceptor
        return attributes != null;
    }

    @Override
    public Map<AiTool, Map<String, Object>> getTools() {
        return aiTools;
    }
}
