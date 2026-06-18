package qingzhou.detector.impl;

import qingzhou.detector.ApplicationProfile;
import java.nio.file.Files;
import java.nio.file.Path;

public class PathDerivationUtil {

    /**
     * 从可执行文件路径或任意路径，向上回溯推导安装根目录
     *
     * @param startPath
     * @param profile
     * @return
     */
    public static Path deriveInstallDir(Path startPath, ApplicationProfile profile) {
        if (startPath == null || !Files.exists(startPath)) {
            return null;
        }

        // 如果是文件，从父目录开始；如果是目录，从自身开始
        Path current = Files.isRegularFile(startPath) ? startPath.getParent() : startPath;

        // 向上回溯最多 5 层
        for (int i = 0; i < 5 && current != null; i++) {
            if (hasConfirmatoryFiles(current, profile)) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    public static boolean hasConfirmatoryFiles(Path dir, ApplicationProfile profile) {
        if (profile.getConfirmatoryFiles() != null) {
            for (String relative : profile.getConfirmatoryFiles()) {
                if (Files.exists(dir.resolve(relative))) {
                    return true;
                }
            }
        }
        return false;
    }
}
