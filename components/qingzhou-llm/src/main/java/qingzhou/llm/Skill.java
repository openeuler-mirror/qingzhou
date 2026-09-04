package qingzhou.llm;

import java.util.Collection;

public interface Skill {
    String name();

    String description();

    // 技能的说明书：激活后注入系统提示词，可用于引导 AI 如何使用该技能下的工具，如果没有工具，那就只是一段提示词增强
    String instruction();

    // 技能的工具集：该技能需要挂载的功能工具
    Collection<Tool> tools();

    /**
     * 是否需按 description 匹配激活。false：参与匹配，命中才激活；true：表示恒激活（跳过匹配直接加载）。
     */
    boolean required();
    
    static Skill of(String name, String description, String instruction, Collection<Tool> tools, boolean required) {
        return new Skill() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return description;
            }

            @Override
            public String instruction() {
                return instruction;
            }

            @Override
            public Collection<Tool> tools() {
                return tools;
            }

            @Override
            public boolean required() {
                return required;
            }
        };
    }
}
