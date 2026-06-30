package qingzhou.llm;

import java.io.IOException;
import java.util.List;

public interface VectorStore {
    // 长文档切块：防止单次注入上下文过长导致超时或超 Token
    void insert(String document, int chunkSize) throws IOException;

    // 从向量库找出最相关的文档块
    List<String> query(String question) throws IOException;
}
