package qingzhou.llm.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import qingzhou.llm.ChatModel;
import qingzhou.llm.Skill;

public class ModelSkillMatcher implements SkillMatcher {
    private static final String NONE = "NONE";
    private final ChatModel selectionModel;

    public ModelSkillMatcher(ChatModel selectionModel) {
        this.selectionModel = selectionModel;
    }

    @Override
    public Collection<Skill> match(Collection<Skill> candidates, String message) {
        StringBuilder prompt = new StringBuilder("以下是可用技能列表，根据用户问题判断应使用哪些技能（可多选、可不选），仅输出技能名称列表（逗号分隔），都不适用则输出 " + NONE + "：");
        for (Skill skill : candidates) {
            prompt.append("\n- ").append(skill.name()).append("：").append(skill.description());
        }
        prompt.append("\n用户问题：").append(message);

        List<Skill> matched = new ArrayList<>();
        String answer = selectionModel.chat(prompt.toString());
        if (answer != null) {
            // 按完整 token 精确匹配而非子串：技能名是通用词（如 "sql"）或恰为 "NONE" 时，子串匹配会误激活
            for (String token : answer.split("[,，\\s]+")) {
                if (token.isEmpty() || NONE.equalsIgnoreCase(token)) continue;
                for (Skill skill : candidates) {
                    if (skill.name().equals(token) && !matched.contains(skill)) {
                        matched.add(skill);
                    }
                }
            }
        }
        return matched;
    }
}
