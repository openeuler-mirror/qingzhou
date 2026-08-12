package qingzhou.llm.impl;

import java.util.Collection;
import java.util.List;

import qingzhou.llm.ChatModel;
import qingzhou.llm.ChatModelFactory;
import qingzhou.llm.Skill;
import qingzhou.llm.Tool;

public abstract class ChatModelBuilderBase implements ChatModelFactory.ChatModelBuilder {
    public final String baseUrl;
    public final String apiKey;
    public final String model;

    public String systemPrompt;
    public List<String> docs;
    public Collection<Tool> tools;
    public Collection<Skill> skills;

    private boolean sealed;

    protected ChatModelBuilderBase(String baseUrl, String apiKey, String model) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
    }

    private void checkSealed() {
        if (sealed) throw new IllegalStateException(this.getClass().getName() + " has been sealed.");
    }

    @Override
    public ChatModelBuilderBase systemPrompt(String systemPrompt) {
        checkSealed();
        this.systemPrompt = systemPrompt;
        return this;
    }

    @Override
    public ChatModelBuilderBase docs(List<String> docs) {
        checkSealed();
        this.docs = docs;
        return this;
    }

    @Override
    public ChatModelBuilderBase tools(Collection<Tool> tools) {
        checkSealed();
        this.tools = tools;
        return this;
    }

    @Override
    public ChatModelBuilderBase skills(Collection<Skill> skills) {
        checkSealed();
        this.skills = skills;
        return this;
    }

    @Override
    public ChatModel build() {
        checkSealed();
        sealed = true;
        return buildInternal();
    }

    protected abstract ChatModel buildInternal();
}
