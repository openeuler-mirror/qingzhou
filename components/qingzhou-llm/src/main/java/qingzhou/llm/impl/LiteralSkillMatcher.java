package qingzhou.llm.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import qingzhou.llm.Skill;

public class LiteralSkillMatcher implements SkillMatcher {
    private static final LiteralSkillMatcher INSTANCE = new LiteralSkillMatcher();

    public static LiteralSkillMatcher getInstance() {
        return INSTANCE;
    }

    /**
     * 问题的扫描范围：词面预筛只检查开头片段即可覆盖绝大多数场景
     */
    private final int MAX_SCAN_LENGTH = 64;
    /**
     * 共享片段的最小长度：中文无空格分词，4 字符以上的连续命中已足以说明词面相关
     */
    private final int MIN_FRAGMENT_LEN = 4;
    private final int MAX_FRAGMENT_LEN = 8;

    private LiteralSkillMatcher() {
    }

    @Override
    public Collection<Skill> match(Collection<Skill> candidates, String message) {
        List<Skill> matched = new ArrayList<>();
        String question = message.length() > MAX_SCAN_LENGTH ? message.substring(0, MAX_SCAN_LENGTH) : message;
        for (Skill skill : candidates) {
            if (isStronglyRelated(skill, question)) {
                matched.add(skill);
            }
        }
        return matched;
    }


    /**
     * 本地词面预筛：技能名直接出现，或与技能描述存在共享的连续片段，即视为强相关
     */
    private boolean isStronglyRelated(Skill skill, String question) {
        String name = skill.name();
        if (question.contains(name)) return true;

        String description = skill.description();
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
