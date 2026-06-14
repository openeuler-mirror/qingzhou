package qingzhou.llm.impl;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;

import org.noear.solon.ai.chat.dialect.ChatDialectManager;
import org.noear.solon.ai.embedding.dialect.EmbeddingDialectManager;
import org.noear.solon.ai.llm.dialect.openai.OpenaiChatDialect;
import org.noear.solon.ai.llm.dialect.openai.OpenaiEmbeddingDialect;
import org.noear.solon.ai.llm.dialect.openai.OpenaiResponsesDialect;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.llm.*;
import qingzhou.llm.impl.log.Slf4jLogBridge;
import qingzhou.logger.Logger;

@Component
public class LLMImpl implements LLM {
    @Reference
    private Logger logger;

    @Activate
    public void init() {
        Slf4jLogBridge.qingzhouLogger = logger; // 放在 solon 加载最前面

        // chat 模型需要
        ChatDialectManager.register(new OpenaiChatDialect());
        ChatDialectManager.register(new OpenaiResponsesDialect());

        // 嵌入模型需求
        EmbeddingDialectManager.register(new OpenaiEmbeddingDialect());
    }

    @Override
    public ChatModel buildChatModel(String baseUrl, String apiKey, String model,
                                    long timeout, String systemPrompt, Collection<Skill> systemSkills) {
        org.noear.solon.ai.chat.ChatModel chatModel = org.noear.solon.ai.chat.ChatModel
                .of(baseUrl)
                .apiKey(apiKey)
                .model(model)
                // 设置超时，防止无限等待
                .timeout(Duration.ofSeconds(timeout))
                // Anthropic Claude 专有参数：开启思考
                .modelOptions(op -> op.optionSet("thinking", new HashMap<String, Object>() {{
                    put("type", "adaptive");
                }}))
                .systemPrompt(systemPrompt)
                .defaultSkillAdd(Converter.convertSkill(systemSkills))
                .build();
        return new ChatModelImpl(chatModel);
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(String baseUrl, String apiKey, String model) {
        org.noear.solon.ai.embedding.EmbeddingModel embeddingModel = org.noear.solon.ai.embedding.EmbeddingModel
                .of(baseUrl)
                .apiKey(apiKey)
                .model(model)
                .build();

        return new EmbeddingModelImpl(embeddingModel);
    }

    @Override
    public RerankingModel buildRerankingModel(String baseUrl, String apiKey, String model) {
        org.noear.solon.ai.reranking.RerankingModel rerankingModel = org.noear.solon.ai.reranking.RerankingModel
                .of(baseUrl)
                .apiKey(apiKey)
                .model(model)
                .build();
        return new RerankingModelImpl(rerankingModel);
    }
}
