package qingzhou.console.util;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileLockUtil {
    private static final Map<File, ReadWriteLock> fileLocks = new ConcurrentHashMap<>();

    public static ReadWriteLock getFileLock(File file) {
        return fileLocks.computeIfAbsent(file, file1 -> new ReentrantReadWriteLock());
    }

    private FileLockUtil() {
    }
}
