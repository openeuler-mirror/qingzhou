package qingzhou.llm;

import java.io.IOException;

public interface EmbeddingModel {
    VectorStore buildVectorStore();

    float[] embed(String content) throws IOException;
}
