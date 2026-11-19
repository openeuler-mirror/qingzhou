package qingzhou.engine.detector;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

import org.osgi.framework.*;
import qingzhou.engine.QingzhouMain;

public class PathDetectionScheduler {
    private static final String DETECTOR_NAME = "qz-path-detector";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // 所有 bundle 的探测任务共用单线程，任务间天然串行
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, DETECTOR_NAME);
        thread.setDaemon(true);
        return thread;
    });

    private final PathDetector detector;
    private final String symbolicName;
    private final String bundleLocation;
    private final String detectedPathKeyPrefix;
    private final Path bundleJar;

    // 序号只作为本进程内的 bundle 身份：同路径跨拍复用同一序号，路径消失即回收该序号，
    // 重启后重新探测、重新取号即可（实例隔离靠不同 bundle 的类加载器，与重启前后序号是否一致无关）
    private final Map<String, Integer> running = new HashMap<>();
    private boolean isAbsenceLogged;

    public PathDetectionScheduler(Bundle bundle) {
        this.detector = new PathDetector(bundle.getHeaders());
        this.symbolicName = bundle.getSymbolicName();
        this.bundleLocation = bundle.getLocation();
        this.detectedPathKeyPrefix = "Qingzhou-Detected-Path." + symbolicName;
        // location 由 toURI() 生成（见 QingzhouMain），须按 URI 解码，否则含空格/中文的路径读不到
        this.bundleJar = Paths.get(URI.create(bundleLocation));
    }

    public void start() {
        executor.scheduleAtFixedRate(this::detectOnce, 0,
                Long.parseLong(System.getProperty("qingzhou.detect.interval", "5")),
                TimeUnit.SECONDS);
    }

    public static void shutdown() {
        executor.shutdownNow();
    }

    private void detectOnce() {
        try {
            Set<String> detected = detector.detect();
            logAbsence(detected);

            List<String> gone = new ArrayList<>(running.keySet());
            gone.removeAll(detected);
            for (String path : gone) stopInstance(path);

            if (detected.size() == 1 && running.isEmpty()) {
                String path = detected.iterator().next();
                int seq = 0;
                startInstance(path, seq);
            } else {
                // 探测结果顺序不稳定（listFiles 与 ps 输出顺序均未定义），新增路径按字典序取号，使分配结果与探测顺序无关
                List<String> added = new ArrayList<>(detected);
                added.removeAll(running.keySet());
                Collections.sort(added);
                Set<Integer> used = new HashSet<>(running.values());
                for (String path : added) {
                    int seq = 1;
                    while (used.contains(seq)) seq++;
                    used.add(seq);
                    startInstance(path, seq);
                }
            }
        } catch (Exception e) {
            // 捕获并打印，保证任务不被取消；引导模块先于日志体系启动，只能直写 stderr
            logWarn(DETECTOR_NAME + " task error: " + e.getMessage());
        }
    }

    // 只在"未探测到 → 探测到"的状态切换处记一条，避免按探测周期重复刷屏；引导期日志体系未就绪，直写 stdout
    private void logAbsence(Set<String> detected) {
        if (!detected.isEmpty()) {
            isAbsenceLogged = false;
            return;
        }
        if (isAbsenceLogged) return;
        isAbsenceLogged = true;
        logInfo(DETECTOR_NAME + ": no instance of " + symbolicName + " detected, keep probing");
    }

    private void stopInstance(String path) {
        try {
            Integer seq = running.get(path);
            QingzhouMain.getBundleContext().getBundle(addSeq(bundleLocation, seq)).stop();
            System.clearProperty(addSeq(detectedPathKeyPrefix, seq));
            running.remove(path);
        } catch (BundleException e) {
            logWarn("App " + symbolicName + " failed to stop, path: " + path + " : " + e.getMessage());
        }
    }

    private void startInstance(String path, int seq) {
        String seqLocation = addSeq(bundleLocation, seq);
        System.setProperty(addSeq(detectedPathKeyPrefix, seq), path);
        BundleContext bundleContext = QingzhouMain.getBundleContext();
        try (InputStream inputStream = Files.newInputStream(bundleJar)) {
            // 同 location 的 bundle 可能来自上个进程或已回收的旧路径，其内容未必对应当前路径，须卸载后重装
            Bundle existing = bundleContext.getBundle(seqLocation);
            if (existing != null) existing.uninstall();
            bundleContext.installBundle(seqLocation, inputStream).start();
            running.put(path, seq);
        } catch (Exception e) {
            // 启动失败不留半成品：清掉探测标记，下一拍按新序号重试
            System.clearProperty(addSeq(detectedPathKeyPrefix, seq));
            logWarn("App " + symbolicName + " failed to start, path: " + path + " : " + e.getMessage());
            Bundle bundle = bundleContext.getBundle(seqLocation);
            if (bundle != null) {
                try {
                    bundle.uninstall();
                } catch (BundleException ex) {
                    logWarn("App " + symbolicName + " failed to uninstall, path: " + path + " : " + e.getMessage());
                }
            }
        }
    }

    private static String addSeq(String str, int seq) {
        if (seq == 0) {
            return str;
        }
        return str + "[" + seq + "]";
    }

    private static void logInfo(String message) {
        System.out.println(LocalDateTime.now().format(TIME_FORMAT) + " [INFO] " + message);
    }

    private static void logWarn(String message) {
        System.err.println(LocalDateTime.now().format(TIME_FORMAT) + " [WARN] " + message);
    }
}
