package qingzhou.llm.impl;

import java.util.Collection;

import qingzhou.llm.ChatModel;
import qingzhou.llm.ChatModelBuilder;
import qingzhou.llm.Skill;
import qingzhou.llm.Tool;

class ChatModelBuilderImpl implements ChatModelBuilder {
    private final org.noear.solon.ai.chat.ChatModel chatModel;

    private String[] docs;
    private Collection<Tool> tools;
    private Collection<Skill> skills;

    ChatModelBuilderImpl(org.noear.solon.ai.chat.ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public ChatModelBuilder withDoc(String[] docs) {
        this.docs = docs;
        return this;
    }

    @Override
    public ChatModelBuilder withTool(Collection<Tool> tools) {
        this.tools = tools;
        return this;
    }

    @Override
    public ChatModelBuilder withSkill(Collection<Skill> skills) {
        this.skills = skills;
        return this;
    }

    @Override
    public ChatModel build() {
        return new ChatModelImpl(chatModel, docs, tools, skills);
    }
}
