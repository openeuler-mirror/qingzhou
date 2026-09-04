package qingzhou.llm.impl;

import java.util.Collection;

import qingzhou.llm.Skill;

/**
 * 技能匹配策略：从候选技能中选出本次对话应激活的技能。
 */
public interface SkillMatcher {
    Collection<Skill> match(Collection<Skill> candidates, String message);
}
