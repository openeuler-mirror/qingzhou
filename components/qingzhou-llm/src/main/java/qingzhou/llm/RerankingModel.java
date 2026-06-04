package qingzhou.llm;


public interface RerankingModel {
    String[] rerank(String query, String[] documents);
}
