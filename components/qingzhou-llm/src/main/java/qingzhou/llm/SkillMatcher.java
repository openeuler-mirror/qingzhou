package qingzhou.llm;

import java.util.Collection;
import java.util.List;

/**
 * 技能匹配策略：从候选技能中选出本次对话应激活的技能。
 * 内置实现 {@code qingzhou.llm.impl.ModelSkillMatcher} 调用大模型选择；
 * 实现本接口可注入自定义匹配策略（通过 ChatModelBuilder.skillMatcher）。
 */
public interface SkillMatcher {
    List<Skill> match(Collection<Skill> candidates, String message);
}
