package qingzhou.path.sniffer;

import java.nio.file.Path;

/**
 * 探测结果
 */
public class PathResult {

    private final Path path;                // 推导出的安装根目录
    private final int confidence;           // 置信度 100:HIGH、50:MEDIUM、20:LOW
    private final SniffStrategy strategy;   // 来源策略，如 ProcessStrategy
    private final String derivedFrom;       // 原始线索，如 "pid:1234, /proc/1234/exe"

    public PathResult(Path path, int confidence, SniffStrategy strategy, String derivedFrom) {
        this.path = path;
        this.confidence = confidence;
        this.strategy = strategy;
        this.derivedFrom = derivedFrom;
    }

    public Path getPath() {
        return path;
    }

    public int getConfidence() {
        return confidence;
    }

    public SniffStrategy getStrategy() {
        return strategy;
    }

    public String getDerivedFrom() {
        return derivedFrom;
    }
}
