package qingzhou.llm;


import java.util.List;

public interface RerankingModel {
    List<String> rerank(String query, List<String> documents);
}
