package qingzhou.store.impl;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import qingzhou.store.Store;

public class FileStore implements Store {
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
        try {
            Files.write(path(key), value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String get(String key) {
        Path path = path(key);
        if (!Files.exists(path)) return null;
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
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
}
