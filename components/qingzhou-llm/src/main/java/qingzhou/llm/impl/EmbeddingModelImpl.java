package qingzhou.llm.impl;

import org.noear.solon.ai.rag.repository.InMemoryRepository;
import qingzhou.llm.EmbeddingModel;
import qingzhou.llm.VectorStore;

class EmbeddingModelImpl implements EmbeddingModel {
    private final org.noear.solon.ai.embedding.EmbeddingModel embeddingModel;

    EmbeddingModelImpl(org.noear.solon.ai.embedding.EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public VectorStore buildVectorStore() {
        // 构建内存向量库
        return new VectorStoreImpl(new InMemoryRepository(embeddingModel));
    }
}
