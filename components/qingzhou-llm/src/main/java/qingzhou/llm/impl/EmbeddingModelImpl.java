package qingzhou.llm.impl;

import java.util.Map;

import org.noear.solon.ai.embedding.dialect.EmbeddingDialectManager;
import org.noear.solon.ai.llm.dialect.openai.OpenaiEmbeddingDialect;
import org.noear.solon.ai.rag.repository.InMemoryRepository;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import qingzhou.llm.EmbeddingModel;
import qingzhou.llm.VectorStore;

@Component(configurationPid = "qingzhou-llm", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class EmbeddingModelImpl implements EmbeddingModel {
    private org.noear.solon.ai.embedding.EmbeddingModel embeddingModel;

    @Activate
    public void init(Map<String, String> config) {
        // 嵌入模型需求
        EmbeddingDialectManager.register(new OpenaiEmbeddingDialect());

        embeddingModel = org.noear.solon.ai.embedding.EmbeddingModel
                .of(config.get("embed.base_url"))
                .apiKey(config.get("embed.api_key"))
                .model(config.get("embed.model"))
                .build();
    }

    @Override
    public VectorStore buildVectorStore() {
        // 构建内存向量库
        return new VectorStoreImpl(new InMemoryRepository(embeddingModel));
    }
}
