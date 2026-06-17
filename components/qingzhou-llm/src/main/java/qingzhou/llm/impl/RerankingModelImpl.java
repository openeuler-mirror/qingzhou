package qingzhou.llm.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.noear.solon.ai.rag.Document;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import qingzhou.llm.RerankingModel;

@Component(configurationPid = "qingzhou-llm", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class RerankingModelImpl implements RerankingModel {
    private org.noear.solon.ai.reranking.RerankingModel rerankingModel;

    @Activate
    public void init(Map<String, String> config) {
        rerankingModel = org.noear.solon.ai.reranking.RerankingModel
                .of(config.get("rerank.base_url"))
                .apiKey(config.get("rerank.api_key"))
                .model(config.get("rerank.model"))
                .build();
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
