package qingzhou.llm.impl;

import java.time.Duration;

import org.noear.solon.ai.chat.dialect.ChatDialectManager;
import org.noear.solon.ai.embedding.dialect.EmbeddingDialectManager;
import org.noear.solon.ai.llm.dialect.openai.OpenaiChatDialect;
import org.noear.solon.ai.llm.dialect.openai.OpenaiEmbeddingDialect;
import org.noear.solon.ai.llm.dialect.openai.OpenaiResponsesDialect;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.llm.ChatModel;
import qingzhou.llm.EmbeddingModel;
import qingzhou.llm.LLM;
import qingzhou.llm.RerankingModel;
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
    public ChatModel buildChatModel(String baseUrl, String apiKey, String model, long timeout, String systemPrompt) {
        org.noear.solon.ai.chat.ChatModel chatModel = org.noear.solon.ai.chat.ChatModel
                .of(baseUrl)
                .apiKey(apiKey)
                .model(model)
                .systemPrompt(systemPrompt)
                .timeout(Duration.ofSeconds(timeout)) // 设置超时，防止无限等待
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
