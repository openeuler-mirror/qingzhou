package qingzhou.ai.skill;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import qingzhou.ai.SkillService;
import qingzhou.ai.ToolService;

abstract class SkillServiceBase implements SkillService {
    private final String[] displayNames;
    private final String description;

    protected final Map<ToolService, Map<String, Object>> aiTools = new ConcurrentHashMap<>();

    protected SkillServiceBase(String[] displayNames, String description) {
        this.displayNames = displayNames;
        this.description = description;
    }

    @Override
    public String[] displayNames() {
        return displayNames;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public Map<ToolService, Map<String, Object>> tools() {
        return aiTools;
    }
}
