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

    @Override
    public void put(String key, String value) {
        Path path = dir.resolve(key);
        try {
            Files.write(path, value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String get(String key) {
        Path path = dir.resolve(key);
        if (!Files.exists(path)) return null;
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(String key) {
        Path path = dir.resolve(key);
        try {
            Files.deleteIfExists(path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean contains(String key) {
        Path path = dir.resolve(key);
        return Files.exists(path);
    }
}
