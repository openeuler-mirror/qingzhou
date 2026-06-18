package qingzhou.detector.impl.strategy;

import qingzhou.detector.ApplicationProfile;
import qingzhou.detector.DetectionStrategy;
import qingzhou.detector.PathResult;
import qingzhou.detector.impl.PathDerivationUtil;
import qingzhou.detector.impl.PlatformUtil;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * 5.候选目录短扫描策略
 *
 * 仅扫描高概率根目录的直接子目录（深度=1），不做全盘递归。 通过目录名特征和确认文件双重验证，快速定位安装路径。
 * 扫描策略： - 深度限制：仅扫描候选根目录的下一级子目录 - 超时限制：整体扫描硬限制 3 秒 - 特征匹配：目录名模糊匹配 + 确认文件存在性验证 - 并行扫描：多个候选根目录并发执行
 *
 * 优先级: 50（兜底策略）
 */
public class CandidateScanStrategy implements DetectionStrategy {

    private static final int PRIORITY = 50;
    private static final long TIMEOUT_SECONDS = 5;

    // Windows 系统盘环境变量名
    private static final String WIN_SYSTEM_DRIVE = "SystemDrive";
    
    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public List<PathResult> detect(ApplicationProfile profile) {
        List<Path> candidateRoots = getCandidateRoots(profile);

        if (candidateRoots.isEmpty()) {
            return Collections.emptyList();
        }

        // 使用线程池并行扫描多个候选根目录，整体受超时控制
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(candidateRoots.size(), 4));

        List<Future<List<PathResult>>> futures = new ArrayList<>();

        for (Path root : candidateRoots) {
            futures.add(executor.submit(() -> scanRoot(root, profile)));
        }

        List<PathResult> allResults = new ArrayList<>();

        try {
            // 整体超时控制：所有扫描任务必须在限定时间内完成
            long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS);

            for (Future<List<PathResult>> future : futures) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    break; // 已超时，不再等待后续结果
                }

                try {
                    List<PathResult> results = future.get(remaining, TimeUnit.MILLISECONDS);
                    if (results != null) {
                        allResults.addAll(results);
                    }
                } catch (TimeoutException e) {
                    future.cancel(true);
                    // 单个根目录扫描超时，继续处理其他结果
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (ExecutionException e) {
                    // 单个扫描任务异常，不影响整体
                }
            }
        } finally {
            executor.shutdownNow();
        }

        profile.deduplicate(allResults);
        return allResults;
    }

    // ==================== 候选根目录获取 ====================
    /**
     * 获取平台相关的高概率候选根目录列表
     */
    private List<Path> getCandidateRoots(ApplicationProfile profile) {
        List<Path> roots = new ArrayList<>();
        if (profile.getCustomCandidatePath() != null) {
            roots.addAll(profile.getCustomCandidatePath());
        }
        if (PlatformUtil.isWindows()) {
            collectWindowsRoots(roots);
        } else if (PlatformUtil.isLinux()) {
            collectLinuxRoots(roots);
        } else if (PlatformUtil.isMac()) {
            collectMacRoots(roots);
        }

        // 过滤不存在的目录
        List<Path> validRoots = new ArrayList<>();
        for (Path root : roots) {
            if (Files.isDirectory(root)) {
                validRoots.add(root);
            }
        }

        return validRoots;
    }

    /**
     * Windows 候选根目录
     */
    private void collectWindowsRoots(List<Path> roots) {
        // 系统盘
        String systemDrive = System.getenv(WIN_SYSTEM_DRIVE);
        if (systemDrive == null || systemDrive.isEmpty()) {
            systemDrive = "C:";
        }

        roots.add(Paths.get(systemDrive, "Program Files"));
        roots.add(Paths.get(systemDrive, "Program Files (x86)"));

        // 用户目录下的可能安装位置
        String userProfile = System.getenv("USERPROFILE");
        if (userProfile != null && !userProfile.isEmpty()) {
            roots.add(Paths.get(userProfile));
        }

        // 当前工作盘符根目录（某些绿色软件直接放根目录）
        roots.add(Paths.get(systemDrive));
    }

    /**
     * Linux 候选根目录
     */
    private void collectLinuxRoots(List<Path> roots) {
        roots.add(Paths.get("/usr/local"));
        roots.add(Paths.get("/opt"));
        roots.add(Paths.get("/usr/share"));
        roots.add(Paths.get("/var/lib"));
        roots.add(Paths.get("/srv"));
        roots.add(Paths.get("/home"));

        // 当前用户主目录
        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isEmpty()) {
            roots.add(Paths.get(userHome));
        }

        // 当前工作目录（某些开发环境直接解压到工作目录）
        String userDir = System.getProperty("user.dir");
        if (userDir != null && !userDir.isEmpty()) {
            roots.add(Paths.get(userDir));
        }
    }

    /**
     * macOS 候选根目录
     */
    private void collectMacRoots(List<Path> roots) {
        roots.add(Paths.get("/usr/local"));          // Intel Mac Homebrew
        roots.add(Paths.get("/opt/homebrew"));       // Apple Silicon Mac Homebrew
        roots.add(Paths.get("/Applications"));     // GUI 应用
        roots.add(Paths.get("/usr/local/Cellar"));   // Homebrew cellar 目录

        // 当前用户主目录
        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isEmpty()) {
            roots.add(Paths.get(userHome));
        }
    }

    // ==================== 单根目录扫描 ====================
    /**
     * 扫描单个候选根目录的直接子目录
     *
     * 仅遍历一层，对每个子目录： 1. 目录名是否匹配应用特征 2. 目录内是否存在确认文件
     */
    private List<PathResult> scanRoot(Path root, ApplicationProfile profile) {
        List<PathResult> results = new ArrayList<>();

        // 快速检查：根目录本身是否就是目标（如 /usr/local/nginx）
        if (matchesDirName(root, profile) && PathDerivationUtil.hasConfirmatoryFiles(root, profile)) {
            results.add(createResult(root, "root-self-match"));
            // 继续扫描子目录，可能存在多版本
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root, Files::isDirectory)) {
            for (Path candidate : stream) {
                // 检查当前线程是否被中断（超时取消）
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }

                // 目录名匹配检查
                if (!matchesDirName(candidate, profile)) {
                    continue;
                }

                // 确认文件存在性检查
                if (PathDerivationUtil.hasConfirmatoryFiles(candidate, profile)) {
                    results.add(createResult(candidate, "dir:" + candidate.getFileName()));
                }
            }
        } catch (IOException e) {
            // 无权限读取或其他IO异常，静默跳过该根目录
        }

        profile.deduplicate(results);
        return results;
    }

    // ==================== 匹配与结果构造 ====================
    /**
     * 检查目录名是否匹配应用特征
     *
     * 支持： - 精确包含：目录名包含任一特征关键字（不区分大小写） - 前缀/后缀匹配：如 tomcat 匹配 apache-tomcat-9.0, tomcat9
     */
    private boolean matchesDirName(Path dir, ApplicationProfile profile) {
        List<String> matches = profile.getDirNameMatches();
        if (matches == null || matches.isEmpty()) {
            return false;
        }

        String dirName = dir.getFileName().toString().toLowerCase();

        for (String pattern : matches) {
            if (pattern == null || pattern.isEmpty()) {
                continue;
            }

            // 简单包含匹配（不区分大小写）
            if (dirName.contains(pattern.toLowerCase())) {
                return true;
            }

            // 处理带版本号的目录名，如 apache-tomcat-9.0.65
            // 特征 "tomcat" 应匹配 "apache-tomcat-9.0.65"
            // 特征 "nginx" 应匹配 "nginx-1.24.0"
            String normalized = dirName.replaceAll("[\\-_\\.\\d]", "");
            if (normalized.contains(pattern.toLowerCase().replaceAll("[\\-_\\.]", ""))) {
                return true;
            }
        }

        return false;
    }

    /**
     * 构造扫描结果
     */
    private PathResult createResult(Path path, String derivedFrom) {
        return new PathResult(path, 20, this, derivedFrom);// 扫描策略置信度最低
    }
}
