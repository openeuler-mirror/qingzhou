package qingzhou.llm.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.noear.solon.ai.rag.Document;
import qingzhou.llm.RerankingModel;

public class RerankingModelImpl implements RerankingModel {
    private final org.noear.solon.ai.reranking.RerankingModel rerankingModel;

    public RerankingModelImpl(org.noear.solon.ai.reranking.RerankingModel rerankingModel) {
        this.rerankingModel = rerankingModel;
    }

    @Override
    public String[] rerank(String query, String[] documents) {
        List<Document> documentList = new ArrayList<>();
        for (String document : documents) {
            documentList.add(new Document(document));
        }
        try {
            List<Document> rerank = rerankingModel.rerank(query, documentList);
            return rerank.stream().map(Document::getContent).toArray(String[]::new);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
