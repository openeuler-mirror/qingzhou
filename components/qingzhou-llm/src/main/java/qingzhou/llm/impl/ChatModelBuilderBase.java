package qingzhou.llm.impl;

import java.util.Collection;
import java.util.List;

import qingzhou.llm.*;

public abstract class ChatModelBuilderBase implements ChatModelFactory.ChatModelBuilder {
    // 全部私有 + getter：apiKey 等敏感配置不得经 public 可变字段被外部直改，
    // sealed 防护才真正生效（否则 setter 校验与 checkSealed 均可被字段写绕过）
    private final String baseUrl;
    private final String apiKey;
    private final String model;

    private String systemPrompt;
    private List<String> docs;
    private ChatMemory chatMemory;
    private Interceptor interceptor;
    private Collection<Tool> tools;
    private Collection<Skill> skills;

    private int maxToolResultChars = 2000;
    private int maxDocChars = 6000;

    private int maxRetries = 4;
    private int maxToolIterations = 20;

    private int connectTimeout = 60 * 1000;
    private int readTimeout = 10 * 60 * 1000;

    private boolean sealed;

    protected ChatModelBuilderBase(String baseUrl, String apiKey, String model) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
    }

    public final String getBaseUrl() {
        return baseUrl;
    }

    public final String getApiKey() {
        return apiKey;
    }

    public final String getModel() {
        return model;
    }

    public final String getSystemPrompt() {
        return systemPrompt;
    }

    public final List<String> getDocs() {
        return docs;
    }

    public final ChatMemory getChatMemory() {
        return chatMemory;
    }

    public final Interceptor getInterceptor() {
        return interceptor;
    }

    public final Collection<Tool> getTools() {
        return tools;
    }

    public final Collection<Skill> getSkills() {
        return skills;
    }

    public final int getMaxToolResultChars() {
        return maxToolResultChars;
    }

    public final int getMaxDocChars() {
        return maxDocChars;
    }

    public final int getMaxRetries() {
        return maxRetries;
    }

    public final int getMaxToolIterations() {
        return maxToolIterations;
    }

    public final int getConnectTimeout() {
        return connectTimeout;
    }

    public final int getReadTimeout() {
        return readTimeout;
    }

    protected void checkSealed() {
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
