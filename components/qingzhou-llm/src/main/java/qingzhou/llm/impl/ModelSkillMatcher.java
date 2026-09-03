package qingzhou.llm.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import qingzhou.llm.ChatModel;
import qingzhou.llm.Skill;
import qingzhou.llm.SkillMatcher;

/**
 * 技能匹配策略：调用大模型匹配。
 * <p>
 * 将候选技能的 description 列表交给模型，由模型根据用户问题选择适用的技能（可多选、可不选），
 * 相比本地关键词/正则匹配，可正确理解语义（如"不需要做系统巡检"不会被误判为激活）。
 * <p>
 * 选择模型由调用方构建并注入（与业务对话相互独立，可复用同一配置）：
 * {@code new ModelSkillMatcher(factory.newChatModelBuilder().build())}。
 * 技能名称须互不包含（如 "Skill" 与 "SystemSkill" 不可共存），否则结果解析会误命中。
 */
public class ModelSkillMatcher implements SkillMatcher {

    private final ChatModel selectionModel;

    public ModelSkillMatcher(ChatModel selectionModel) {
        this.selectionModel = selectionModel;
    }

    @Override
    public List<Skill> match(Collection<Skill> candidates, String message) {
        List<Skill> matched = new ArrayList<>();
        if (candidates == null || candidates.isEmpty() || message == null || message.isEmpty()) return matched;

        StringBuilder prompt = new StringBuilder("以下是可用技能列表，根据用户问题判断应使用哪些技能（可多选、可不选），仅输出技能名称列表（逗号分隔），都不适用则输出 NONE：");
        for (Skill skill : candidates) {
            prompt.append("\n- ").append(skill.name()).append("：").append(skill.description());
        }
        prompt.append("\n用户问题：").append(message);

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
