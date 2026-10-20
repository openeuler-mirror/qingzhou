package qingzhou.app.redis.store;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import qingzhou.api.AppContext;
import qingzhou.json.Json;
import qingzhou.logger.Logger;

/**
 * 存储快照文件：统一数据目录、JSON 读写与失败日志。
 */
final class SnapshotFile {
    private final AppContext appContext;
    private final Path path;
    private final Logger logger;

    SnapshotFile(AppContext appContext, String fileName) {
        this.appContext = appContext;
        this.logger = appContext.getService(Logger.class);
        File dir = new File(appContext.getRoot(), StoreConstants.DATA_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            warn("cannot create data directory: " + dir);
        }
        this.path = new File(dir, fileName).toPath();
    }

    Path path() {
        return path;
    }

    void save(Object snapshot) {
        Json json = json();
        if (json == null) return;
        try {
            Files.write(path, json.toJson(snapshot).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            warn("failed to save snapshot: " + path.getFileName(), e);
        }
    }

    <T> T load(Class<T> type) {
        if (!Files.exists(path)) return null;
        Json json = json();
        if (json == null) return null;
        try {
            String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            return json.fromJson(text, type);
        } catch (Exception e) {
            warn("failed to load snapshot: " + path.getFileName(), e);
            return null;
        }
    }

    void warn(String message) {
        if (logger != null) logger.warn(message);
    }

    void warn(String message, Exception e) {
        if (logger != null) logger.warn(message, e);
    }

    Json json() {
        return appContext.getService(Json.class);
    }
}
