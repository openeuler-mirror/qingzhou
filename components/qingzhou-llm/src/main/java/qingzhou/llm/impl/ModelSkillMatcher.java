package qingzhou.llm.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import qingzhou.llm.ChatModel;
import qingzhou.llm.Skill;

public class ModelSkillMatcher implements SkillMatcher {
    private final ChatModel selectionModel;

    public ModelSkillMatcher(ChatModel selectionModel) {
        this.selectionModel = selectionModel;
    }

    @Override
    public Collection<Skill> match(Collection<Skill> candidates, String message) {
        StringBuilder prompt = new StringBuilder("以下是可用技能列表，根据用户问题判断应使用哪些技能（可多选、可不选），仅输出技能名称列表（逗号分隔），都不适用则输出 NONE：");
        for (Skill skill : candidates) {
            prompt.append("\n- ").append(skill.name()).append("：").append(skill.description());
        }
        prompt.append("\n用户问题：").append(message);

        List<Skill> matched = new ArrayList<>();
        String answer = selectionModel.chat(prompt.toString());
        if (answer != null) {
            for (Skill skill : candidates) {
                if (answer.contains(skill.name())) {
                    matched.add(skill);
                }
            }
        }
        return matched;
    }
}
