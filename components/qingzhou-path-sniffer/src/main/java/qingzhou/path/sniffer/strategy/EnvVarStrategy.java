package qingzhou.path.sniffer.strategy;

import qingzhou.path.sniffer.ApplicationProfile;
import qingzhou.path.sniffer.SniffStrategy;
import qingzhou.path.sniffer.PathResult;
import qingzhou.path.sniffer.util.PathDerivationUtil;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 1.环境变量探测策略
 *
 * 通过读取系统环境变量（如 CATALINA_HOME、NGINX_HOME 等）， 直接获取应用安装路径，零外部命令开销。
 * 验证逻辑： 1. 环境变量值直接指向安装目录（包含确认文件）→ 直接采纳 2. 环境变量指向子目录（如 bin/）→ 通过回溯推导安装根目录 3. 路径不存在或无法验证 → 丢弃
 * 置信度: CERTAIN（环境变量为显式配置，可靠性最高） 优先级: 10（最高优先级，零开销且最可信）
 */
public class EnvVarStrategy implements SniffStrategy {

    private static final int PRIORITY = 10;

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public List<PathResult> sniff(ApplicationProfile profile) {
        List<String> envKeys = profile.getEnvVarKeys();
        if (envKeys == null || envKeys.isEmpty()) {
            return Collections.emptyList();
        }

        List<PathResult> results = new ArrayList<>();

        for (String key : envKeys) {
            String value = System.getenv(key);
            if (value == null || value.trim().isEmpty()) {
                continue;
            }

            Path path = Paths.get(value.trim());

            // 路径必须存在（文件或目录均可，deriveInstallDir 会处理）
            if (!Files.exists(path)) {
                continue;
            }

            Path installDir;
            // 情况1: 环境变量直接指向安装根目录
            if (PathDerivationUtil.hasConfirmatoryFiles(path, profile)) {
                installDir = path;
            } else {
                // 情况2: 环境变量指向子目录（如 bin/、sbin/）或文件，向上回溯推导
                installDir = PathDerivationUtil.deriveInstallDir(path, profile);
            }

            if (installDir != null) {
                results.add(new PathResult(installDir, 100, this, "env:" + key + "=" + value));
            }
        }

        profile.deduplicate(results);
        return results;
    }

}
