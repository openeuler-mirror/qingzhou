package qingzhou.common;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 临时目录清理工具：平台各模块上传、缓存残留文件的清理入口。
 * 只做"清空目录"与"按 TTL 清理过期项"，不含任何业务策略（TTL 由调用方给出）。
 */
public final class TempFiles {
    // 清理最小间隔：避免每个请求都扫描上传目录（高并发下重复 IO）
    private static final long CLEAN_MIN_INTERVAL_MILLIS = 60_000;
    // 按目录限流：不同目录互不抑制，避免低流量目录长期得不到清理
    private static final Map<String, Long> lastCleanAtByDir = new ConcurrentHashMap<>();

    /**
     * 删除目录下最后修改时间早于 {@code now - ttlMillis} 的子项（递归删除）；目录不存在或无法列举时静默跳过。
     */
    public static void cleanExpired(File dir, long ttlMillis) {
        if (dir == null) return;

        long now = System.currentTimeMillis();
        String dirKey = dir.getAbsolutePath();
        Long lastCleanAt = lastCleanAtByDir.get(dirKey);
        if (lastCleanAt != null && now - lastCleanAt < CLEAN_MIN_INTERVAL_MILLIS) return;
        lastCleanAtByDir.put(dirKey, now);

        File[] children = dir.listFiles();
        if (children == null) return;
        long expireBefore = now - ttlMillis;
        for (File child : children) {
            if (child.lastModified() < expireBefore) {
                FileUtil.forceDeleteQuietly(child);
            }
        }
    }
}
