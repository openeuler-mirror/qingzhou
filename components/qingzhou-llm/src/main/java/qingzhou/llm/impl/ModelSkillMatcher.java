package qingzhou.llm.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import qingzhou.llm.ChatModel;
import qingzhou.llm.Skill;

/**
 * 技能匹配策略：本地词面预筛 + 大模型语义兜底。
 * <p>先做零开销的本地判定：用户问题与候选技能存在强词面关联（技能名直接出现，
 * 或与技能描述共享较长的连续片段）时直接激活，省去一整轮模型请求；
 * 无词面依据时才调用模型做语义判断（可正确理解“不需要做系统巡检”这类否定表达），
 * 兼顾首字延迟与匹配准确度。词面命中时的误激活（如否定句）仅多挂载工具定义，
 * 模型不会被强制调用，风险可控。</p>
 */
public class ModelSkillMatcher implements SkillMatcher {

    /** 问题的扫描范围：词面预筛只检查开头片段即可覆盖绝大多数场景 */
    private static final int MAX_SCAN_LENGTH = 64;
    /** 共享片段的最小长度：中文无空格分词，4 字符以上的连续命中已足以说明词面相关 */
    private static final int MIN_FRAGMENT_LEN = 4;
    private static final int MAX_FRAGMENT_LEN = 8;

    private final ChatModel selectionModel;

    public ModelSkillMatcher(ChatModel selectionModel) {
        this.selectionModel = selectionModel;
    }

    @Override
    public Collection<Skill> match(Collection<Skill> candidates, String message) {
        List<Skill> matched = new ArrayList<>();
        if (candidates == null || candidates.isEmpty() || message == null || message.isEmpty()) return matched;

        String question = message.length() > MAX_SCAN_LENGTH ? message.substring(0, MAX_SCAN_LENGTH) : message;
        for (Skill skill : candidates) {
            if (isStronglyRelated(skill, question)) {
                matched.add(skill);
            }
        }
        if (!matched.isEmpty()) return matched; // 词面强相关：直接激活，不再调用模型

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

    /** 本地词面预筛：技能名直接出现，或与技能描述存在共享的连续片段，即视为强相关 */
    private static boolean isStronglyRelated(Skill skill, String question) {
        String name = skill.name();
        if (name != null && !name.isEmpty() && question.contains(name)) return true;

        String description = skill.description();
        if (description == null || description.isEmpty()) return false;
        if (description.length() > MAX_SCAN_LENGTH) description = description.substring(0, MAX_SCAN_LENGTH);

        // 问题中任意 4~8 字符的连续片段出现在技能描述里，说明二者提到了同一概念
        int maxLen = Math.min(MAX_FRAGMENT_LEN, question.length());
        for (int len = MIN_FRAGMENT_LEN; len <= maxLen; len++) {
            for (int i = 0; i + len <= question.length(); i++) {
                if (description.contains(question.substring(i, i + len))) {
                    return true;
                }
            }
        }
        return false;
    }
}
