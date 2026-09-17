package qingzhou.store.impl;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import qingzhou.store.Store;

public class FileStore implements Store {
    private static final String TMP_FILE_PREFIX = ".tmp-";
    private final Path dir;

    public FileStore(File baseDir) {
        this.dir = baseDir.toPath();
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Path path(String key) {
        Path path = dir.resolve(key).normalize();
        if (!path.startsWith(dir)) throw new IllegalArgumentException("illegal store key: " + key);
        return path;
    }

    @Override
    public void put(String key, String value) {
        // 先写临时文件再原子替换：全程不触碰目标，进程崩溃/断电后目标始终是完整的旧值或新值，
        // 不会留下半截内容。临时文件与目标同目录，保证同一文件系统上可做原子移动
        Path path = path(key);
        Path temp = dir.resolve(TMP_FILE_PREFIX + UUID.randomUUID());
        try {
            Files.write(temp, value.getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); // 文件系统不支持原子替换时退化为普通移动
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try { // move 已成功则此文件不存在；写失败或 move 失败时清理残留
                Files.deleteIfExists(temp);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public String get(String key) {
        try {
            return new String(Files.readAllBytes(path(key)), StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            return null; // 并发删除时读不到即为不存在，且消除 exists-then-read 竞态
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(path(key));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean contains(String key) {
        return Files.exists(path(key));
    }

    @Override
    public Set<String> keys() {
        try (Stream<Path> files = Files.list(dir)) {
            // 跳过 put 的写中临时文件（崩溃时可能残留），不属于存储的 key
            return files.map(path -> path.getFileName().toString())
                    .filter(name -> !name.startsWith(TMP_FILE_PREFIX))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
