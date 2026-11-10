package qingzhou.llm;

import java.util.Collection;
import java.util.function.Supplier;

import qingzhou.llm.impl.Lazy;

public interface Skill {
    String name();

    String description();

    // 技能的说明书：激活后注入系统提示词，可用于引导 AI 如何使用该技能下的工具，如果没有工具，那就只是一段提示词增强
    String instruction();

    // 技能的工具集：该技能需要挂载的功能工具
    Collection<Tool> tools();

    // provider 延迟到 getter 首次调用时执行：匹配阶段只用 name/description，未激活技能不触发说明书与工具集
    static Skill of(String name, String description,
                    Supplier<String> instructionProvider,
                    Supplier<Collection<Tool>> toolsProvider) {
        Supplier<String> instruction = new Lazy<>(instructionProvider);
        Supplier<Collection<Tool>> tools = new Lazy<>(toolsProvider);
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
                return instruction.get();
            }

            @Override
            public Collection<Tool> tools() {
                return tools.get();
            }
        };
    }
}
