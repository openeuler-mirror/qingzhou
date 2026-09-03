package qingzhou.llm.impl;

import java.util.Collection;
import java.util.List;

import qingzhou.llm.*;

public abstract class ChatModelBuilderBase implements ChatModelFactory.ChatModelBuilder {
    public final String baseUrl;
    public final String apiKey;
    public final String model;

    public String systemPrompt;
    public ReasoningEffort reasoningEffort;
    public List<String> docs;
    public Collection<Tool> tools;
    public Collection<Tool> dynamicTool;
    public Collection<Skill> skills;
    public String imageDetail;

    public int maxToolResultChars = 2000;
    public int maxPerRefChars = 6000;

    public int maxRetries = 3;
    public int maxToolIterations = 20;

    public int connectTimeout = 60 * 1000;
    public int readTimeout = 10 * 60 * 1000;

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
    public ChatModelFactory.ChatModelBuilder reasoningEffort(ReasoningEffort reasoningEffort) {
        checkSealed();
        this.reasoningEffort = reasoningEffort;
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
    public ChatModelFactory.ChatModelBuilder dynamicTool(Collection<Tool> tools) {
        checkSealed();
        this.dynamicTool = tools;
        return this;
    }

    @Override
    public ChatModelBuilderBase skills(Collection<Skill> skills) {
        checkSealed();
        this.skills = skills;
        return this;
    }

    @Override
    public ChatModelFactory.ChatModelBuilder imageDetail(ChatModelFactory.ImageDetail imageDetail) {
        checkSealed();
        if (imageDetail != null) {
            this.imageDetail = imageDetail.name();
        }
        return this;
    }

    @Override
    public ChatModelFactory.ChatModelBuilder maxToolResultChars(int maxToolResultChars) {
        checkSealed();
        if (maxToolResultChars > 0) {
            this.maxToolResultChars = maxToolResultChars;
        }
        return this;
    }

    @Override
    public ChatModelFactory.ChatModelBuilder maxPerRefChars(int maxPerRefChars) {
        checkSealed();
        if (maxPerRefChars > 0) {
            this.maxPerRefChars = maxPerRefChars;
        }
        return this;
    }

    @Override
    public ChatModelFactory.ChatModelBuilder maxRetries(int maxRetries) {
        checkSealed();
        if (maxRetries > 0) {
            this.maxRetries = maxRetries;
        }
        return this;
    }

    @Override
    public ChatModelFactory.ChatModelBuilder maxToolIterations(int maxToolIterations) {
        checkSealed();
        if (maxToolIterations > 0) {
            this.maxToolIterations = maxToolIterations;
        }
        return this;
    }

    @Override
    public ChatModelFactory.ChatModelBuilder connectTimeout(int connectTimeout) {
        checkSealed();
        if (connectTimeout < 0) {
            throw new IllegalArgumentException("timeout can not be negative");
        }
        this.connectTimeout = connectTimeout;
        return this;
    }

    @Override
    public ChatModelFactory.ChatModelBuilder readTimeout(int readTimeout) {
        checkSealed();
        if (readTimeout < 0) {
            throw new IllegalArgumentException("timeout can not be negative");
        }
        this.readTimeout = readTimeout;
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
