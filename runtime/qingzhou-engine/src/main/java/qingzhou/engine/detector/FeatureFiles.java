package qingzhou.engine.detector;

import java.io.File;
import java.util.List;

// 判定候选目录是否含任一特征文件；文件策略与进程策略共用同一判定
class FeatureFiles {
    private final List<String> names;

    FeatureFiles(List<String> names) {
        this.names = names;
    }

    boolean containsIn(File dir) {
        return names.stream().anyMatch(name -> new File(dir, name).exists());
    }
}
