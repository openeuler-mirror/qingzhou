package qingzhou.app.nginx;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 配置/日志文件的枚举、安全访问与文本读写。
 *
 * 文件 ID 由文件名生成；重名时附加路径哈希，保证同一模型内唯一且不把路径直接暴露到 URL。
 */
final class NginxFiles {
    // 框架单参数上限 1 MB；留出表单开销后作为在线编辑上限
    static final int MAX_CONFIG_BYTES = 1024 * 1024 - 4096;
    static final int MAX_LOG_VIEW_BYTES = 2 * 1024 * 1024;
    private static final int MAX_FILES = 500;
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private NginxFiles() {
    }

    static List<Entry> listConfigs(NginxInstall install) {
        Set<Path> roots = new LinkedHashSet<>();
        Path confDir = install.conf.getParent();
        if (confDir != null) roots.add(confDir);
        roots.add(install.prefix.resolve("conf"));
        roots.add(install.prefix.resolve("sites-available"));
        roots.add(install.prefix.resolve("sites-enabled"));

        List<Path> files = new ArrayList<>();
        for (Path root : roots) {
            collectFiles(root, 4,
                    path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".conf"), files);
        }
        return buildEntries(install, files);
    }

    static List<Entry> listLogs(NginxInstall install) {
        List<Path> files = new ArrayList<>();
        for (Path root : install.logDirs) {
            collectFiles(root, 2, NginxFiles::isLogFile, files);
        }
        return buildEntries(install, files);
    }

    static Entry find(List<Entry> entries, String id) {
        for (Entry entry : entries) {
            if (entry.id.equals(id)) return entry;
        }
        return null;
    }

    static String readText(Path file, int maxBytes) {
        long size = fileSize(file);
        if (size > maxBytes) {
            throw new IllegalStateException("文件大小为 " + formatSize(size)
                    + "，超过在线查看上限 " + formatSize(maxBytes) + ": " + file);
        }
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("读取文件失败: " + file, e);
        }
    }

    /**
     * 大日志只读尾部窗口：截断处可能落在半个字符上，跳到首个换行之后。
     */
    static String tailText(Path file, int maxBytes) {
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            long length = raf.length();
            if (length <= maxBytes) {
                byte[] content = new byte[(int) length];
                raf.readFully(content);
                return new String(content, StandardCharsets.UTF_8);
            }

            byte[] tail = new byte[maxBytes];
            raf.seek(length - tail.length);
            raf.readFully(tail);
            int start = 0;
            while (start < tail.length && tail[start] != '\n') start++;
            start = Math.min(start + 1, tail.length);
            return "...(仅显示最后 " + formatSize(maxBytes) + ")\n"
                    + new String(tail, start, tail.length - start, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("读取日志文件失败: " + file, e);
        }
    }

    /**
     * 配置可能是主配置，也可能是被 include 的片段，不能单独当主配置校验：
     * 因此先备份并原子替换目标文件，再执行完整的 nginx -t；失败则用原内容回滚。
     * 替换前写 pending journal，进程在替换与回滚之间退出时由启动恢复兜底。
     */
    static void updateConfig(NginxService service, Entry entry, String content, Path backupDir) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("配置内容不能为空");
        }
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_CONFIG_BYTES) {
            throw new IllegalArgumentException("配置内容超过 " + formatSize(MAX_CONFIG_BYTES) + " 上限");
        }

        synchronized (NginxService.PROCESS_LOCK) {
            Path realFile;
            byte[] original;
            try {
                realFile = entry.path.toRealPath();
            } catch (IOException e) {
                throw new IllegalStateException("配置文件不存在或无法访问: " + entry.path, e);
            }
            if (!service.getInstall().isAllowed(realFile, false)) {
                throw new IllegalStateException("配置文件不在允许编辑的目录内: " + realFile);
            }
            try {
                long length = Files.size(realFile);
                if (length > MAX_CONFIG_BYTES) {
                    throw new IllegalArgumentException("文件超过 " + formatSize(MAX_CONFIG_BYTES) + " 上限，不支持在线编辑");
                }
                original = Files.readAllBytes(realFile);
            } catch (IOException e) {
                throw new IllegalStateException("读取配置文件失败: " + realFile, e);
            }

            Path backup = backup(realFile, backupDir);
            Path journal = journalPath(backup);
            writeJournal(journal, realFile);

            Path temp = null;
            boolean replaced = false;
            try {
                temp = Files.createTempFile(realFile.getParent(), ".nginx-qz-", ".tmp");
                Files.write(temp, bytes, StandardOpenOption.TRUNCATE_EXISTING);
                forceFile(temp);
                preservePermissions(realFile, temp);
                replaced = true;
                replace(temp, realFile);
                temp = null;

                NginxCommand.Result test;
                try {
                    test = service.test();
                } catch (RuntimeException e) {
                    restore(realFile, original, backup);
                    deleteQuietly(journal);
                    throw e;
                }
                if (!test.isSuccess()) {
                    restore(realFile, original, backup);
                    deleteQuietly(journal);
                    throw new IllegalArgumentException("配置校验失败，已回滚: " + test.toMessage());
                }
                deleteQuietly(journal);
            } catch (IOException e) {
                if (replaced) {
                    try {
                        restore(realFile, original, backup);
                    } catch (RuntimeException restoreFailed) {
                        throw new IllegalStateException("保存配置失败且回滚失败，请从备份恢复: " + backup, restoreFailed);
                    }
                }
                deleteQuietly(journal);
                throw new IllegalStateException("保存 nginx 配置失败: " + realFile, e);
            } finally {
                if (temp != null) {
                    try {
                        Files.deleteIfExists(temp);
                    } catch (IOException ignored) {
                        // 临时文件清理失败不影响已确定的业务结果
                    }
                }
            }
        }
    }

    /**
     * 进程在替换与回滚之间退出时，下一次应用启动用备份恢复原配置。
     */
    static void recoverPendingBackups(Path backupDir) {
        if (!Files.isDirectory(backupDir)) return;
        try (DirectoryStream<Path> journals = Files.newDirectoryStream(backupDir, "*.pending")) {
            for (Path journal : journals) {
                String targetText;
                try {
                    targetText = new String(Files.readAllBytes(journal), StandardCharsets.UTF_8).trim();
                } catch (IOException e) {
                    continue;
                }
                if (targetText.isEmpty()) {
                    deleteQuietly(journal);
                    continue;
                }
                Path target;
                Path backup;
                try {
                    target = java.nio.file.Paths.get(targetText);
                    String name = journal.getFileName().toString();
                    backup = journal.resolveSibling(name.substring(0, name.length() - ".pending".length()));
                } catch (RuntimeException e) {
                    deleteQuietly(journal);
                    continue;
                }
                if (!Files.isRegularFile(backup) || !Files.isRegularFile(target)) {
                    deleteQuietly(journal);
                    continue;
                }
                try {
                    Files.copy(backup, target, StandardCopyOption.REPLACE_EXISTING);
                    deleteQuietly(journal);
                } catch (IOException ignored) {
                    // 保留 journal，下次启动继续尝试
                }
            }
        } catch (IOException ignored) {
            // 备份目录不可读时无法恢复，交由管理员按备份文件处理
        }
    }

    private static long fileSize(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            throw new IllegalStateException("读取文件大小失败: " + file, e);
        }
    }

    private static String formatTime(FileTime time) {
        LocalDateTime local = LocalDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault());
        return TIME_FORMAT.format(local);
    }

    static String formatSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format(Locale.ROOT, "%.1f KB", size / 1024.0);
        if (size < 1024L * 1024 * 1024) return String.format(Locale.ROOT, "%.1f MB", size / (1024.0 * 1024));
        return String.format(Locale.ROOT, "%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    static int lineCount(String text) {
        if (text.isEmpty()) return 0;
        int lines = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') lines++;
        }
        return text.charAt(text.length() - 1) == '\n' ? lines : lines + 1;
    }

    private static void collectFiles(Path root, int maxDepth, Predicate<Path> filter, List<Path> files) {
        if (!Files.isDirectory(root)) return;
        try {
            Path realRoot = root.toRealPath();
            Files.walkFileTree(realRoot, Collections.emptySet(), maxDepth, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (!isHidden(file) && Files.isRegularFile(file) && filter.test(file)) files.add(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE; // 单个不可读/断链项跳过，不影响整页
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("遍历目录失败: " + root, e);
        }
    }

    private static List<Entry> buildEntries(NginxInstall install, List<Path> candidates) {
        List<Path> files = new ArrayList<>(new LinkedHashSet<>(candidates));
        files.sort((left, right) -> install.display(left).compareToIgnoreCase(install.display(right)));
        if (files.size() > MAX_FILES) files = new ArrayList<>(files.subList(0, MAX_FILES));

        java.util.Map<String, Integer> nameCounts = new java.util.HashMap<>();
        for (Path path : files) {
            String name = path.getFileName().toString();
            nameCounts.put(name, nameCounts.containsKey(name) ? nameCounts.get(name) + 1 : 1);
        }

        Set<String> usedIds = new HashSet<>();
        List<Entry> entries = new ArrayList<>(files.size());
        for (Path path : files) {
            String name = path.getFileName().toString();
            // 重名文件用路径哈希做后缀，避免文件增删导致同一个 ID 指向另一个文件
            String id = nameCounts.get(name) > 1 ? name + "~" + pathHash(path) : name;
            String base = id;
            int sequence = 2;
            while (usedIds.contains(id)) {
                id = base + "~" + sequence++;
            }
            usedIds.add(id);

            long size;
            String modified;
            try {
                size = Files.size(path);
                modified = formatTime(Files.getLastModifiedTime(path));
            } catch (IOException e) {
                continue; // 枚举与 stat 之间被删除/轮转的文件跳过，不让整页失败
            }
            Path parent = path.getParent();
            entries.add(new Entry(id, path, parent == null ? "" : install.display(parent), size, modified));
        }
        return entries;
    }

    private static String pathHash(Path path) {
        return Integer.toHexString(path.toAbsolutePath().normalize().toString().hashCode());
    }

    private static boolean isHidden(Path path) {
        Path name = path.getFileName();
        return name == null || name.toString().startsWith(".");
    }

    private static boolean isLogFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.contains(".log") && !name.endsWith(".gz");
    }

    private static Path backup(Path file, Path backupDir) {
        try {
            Files.createDirectories(backupDir);
            String name = file.getFileName() + "." + Integer.toHexString(file.toString().hashCode()) + ".bak";
            Path backup = backupDir.resolve(name);
            Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.setPosixFilePermissions(backup,
                        java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException | IOException ignored) {
                // Windows 或无 POSIX 权限的文件系统
            }
            return backup;
        } catch (IOException e) {
            throw new IllegalStateException("备份 nginx 配置失败: " + file, e);
        }
    }

    private static Path journalPath(Path backup) {
        return backup.resolveSibling(backup.getFileName() + ".pending");
    }

    private static void writeJournal(Path journal, Path target) {
        try {
            Files.write(journal, target.toString().getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            forceFile(journal);
        } catch (IOException e) {
            throw new IllegalStateException("写入配置恢复日志失败: " + journal, e);
        }
    }

    private static void forceFile(Path file) {
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
            channel.force(true);
        } catch (IOException ignored) {
            // 部分文件系统不支持 fsync，保留尽力而为的持久化
        }
    }

    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // journal 清理失败时下次启动会再次尝试恢复
        }
    }

    private static void restore(Path target, byte[] original, Path backup) {
        Path temp = null;
        try {
            temp = Files.createTempFile(target.getParent(), ".nginx-qz-", ".tmp");
            Files.write(temp, original, StandardOpenOption.TRUNCATE_EXISTING);
            preservePermissions(target, temp);
            replace(temp, target);
            temp = null;
        } catch (IOException e) {
            throw new IllegalStateException("配置校验失败且回滚失败，请从备份恢复: " + backup, e);
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // 回滚后的临时文件清理失败不影响原异常
                }
            }
        }
    }

    private static void preservePermissions(Path source, Path target) {
        try {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(source);
            Files.setPosixFilePermissions(target, permissions);
        } catch (UnsupportedOperationException | IOException ignored) {
            // Windows 或无 POSIX 权限的文件系统没有可保留的权限
        }
    }

    private static void replace(Path source, Path target) {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicMoveFailed) {
            try {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new IllegalStateException("替换 nginx 配置文件失败: " + target, e);
            }
        }
    }

    static final class Entry {
        final String id;
        final Path path;
        final String dir;
        final long size;
        final String modified;

        Entry(String id, Path path, String dir, long size, String modified) {
            this.id = id;
            this.path = path;
            this.dir = dir;
            this.size = size;
            this.modified = modified;
        }
    }
}
