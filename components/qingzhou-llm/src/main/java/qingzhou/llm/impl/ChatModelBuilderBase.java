package qingzhou.llm.impl;

import java.util.Collection;
import java.util.List;

import qingzhou.llm.*;

public abstract class ChatModelBuilderBase implements ChatModelFactory.ChatModelBuilder {
    public final String baseUrl;
    public final String apiKey;
    public final String model;

    public String systemPrompt;
    public List<String> docs;
    public ChatMemory chatMemory;
    public Interceptor interceptor;
    public Collection<Tool> tools;
    public Collection<Skill> skills;

    public int maxToolResultChars = 2000;
    public int maxDocChars = 6000;

    public int maxRetries = 4;
    public int maxToolIterations = 20;

    public int connectTimeout = 60 * 1000;
    public int readTimeout = 10 * 60 * 1000;

    private boolean sealed;

    protected ChatModelBuilderBase(String baseUrl, String apiKey, String model) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
    }

    public void checkSealed() {
        if (sealed) throw new IllegalStateException(this.getClass().getSimpleName() + " has been sealed.");
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
    public ChatModelFactory.ChatModelBuilder chatMemory(ChatMemory chatMemory) {
        checkSealed();
        this.chatMemory = chatMemory;
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
    public ChatModelFactory.ChatModelBuilder maxToolResultChars(int maxToolResultChars) {
        checkSealed();
        if (maxToolResultChars < 0) throw new IllegalArgumentException("maxToolResultChars can not be negative");
        this.maxToolResultChars = maxToolResultChars;
        return this;
    }

    @Override
    public ChatModelFactory.ChatModelBuilder maxDocChars(int maxDocChars) {
        checkSealed();
        if (maxDocChars < 0) throw new IllegalArgumentException("maxDocChars can not be negative");
        this.maxDocChars = maxDocChars;
        return this;
    }

    @Override
    public ChatModelFactory.ChatModelBuilder maxRetries(int maxRetries) {
        checkSealed();
        if (maxRetries < 0) throw new IllegalArgumentException("maxRetries can not be negative");
        this.maxRetries = maxRetries;
        return this;
    }

    @Override
    public ChatModelFactory.ChatModelBuilder maxToolIterations(int maxToolIterations) {
        checkSealed();
        if (maxToolIterations < 0) throw new IllegalArgumentException("maxToolIterations can not be negative");
        this.maxToolIterations = maxToolIterations;
        return this;
    }

    @Override
    public ChatModelFactory.ChatModelBuilder connectTimeout(int connectTimeout) {
        checkSealed();
        if (connectTimeout < 0) throw new IllegalArgumentException("connectTimeout can not be negative");
        this.connectTimeout = connectTimeout;
        return this;
    }

    @Override
    public ChatModelFactory.ChatModelBuilder readTimeout(int readTimeout) {
        checkSealed();
        if (readTimeout < 0) throw new IllegalArgumentException("readTimeout can not be negative");
        this.readTimeout = readTimeout;
        return this;
    }

    @Override
    public ChatModelFactory.ChatModelBuilder interceptor(Interceptor interceptor) {
        checkSealed();
        this.interceptor = interceptor;
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
