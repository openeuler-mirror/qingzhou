package qingzhou.llm.impl;

import java.io.IOException;
import java.util.List;

import org.noear.solon.ai.rag.Document;
import org.noear.solon.ai.rag.repository.InMemoryRepository;
import org.noear.solon.ai.rag.splitter.TokenSizeTextSplitter;
import qingzhou.llm.VectorStore;

class VectorStoreImpl implements VectorStore {
    private final InMemoryRepository repository;

    VectorStoreImpl(InMemoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public void insert(String document, int chunkSize) throws IOException {
        // 将文档按大约 chunkSize Token 大小切分，允许 chunkSize / 10 Token 重叠保证语义连贯等
        TokenSizeTextSplitter splitter = new TokenSizeTextSplitter(chunkSize, chunkSize / 10);
        List<Document> chunks = splitter.split(document);
        repository.save(chunks); // 向量化并入库 (InMemoryRepository 会自动调用嵌入模型将文本转为向量)
    }

    @Override
    public String[] query(String question) throws IOException {
        List<Document> searched = repository.search(question);
        return searched.stream().map(Document::getContent).toArray(String[]::new);
    }
}
