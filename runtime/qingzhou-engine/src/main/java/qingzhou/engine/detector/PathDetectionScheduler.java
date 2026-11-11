package qingzhou.engine.detector;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import qingzhou.engine.QingzhouMain;

public class PathDetectionScheduler {
    private static final String DETECTOR_NAME = "qz-path-detector";
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

            List<String> gone = new ArrayList<>(running.keySet());
            gone.removeAll(detected);
            for (String path : gone) stopInstance(running.remove(path));

            // 探测结果顺序不稳定（listFiles 与 ps 输出顺序均未定义），新增路径按字典序取号，使分配结果与探测顺序无关
            List<String> added = new ArrayList<>(detected);
            added.removeAll(running.keySet());
            Collections.sort(added);
            Set<Integer> used = new HashSet<>(running.values());
            for (String path : added) startInstance(path, used);
        } catch (Exception e) {
            // 捕获并打印，保证任务不被取消；引导模块先于日志体系启动，只能直写 stderr
            System.err.println(DETECTOR_NAME + " task error: " + e.getMessage());
        }
    }

    private void stopInstance(int seq) throws BundleException {
        System.clearProperty(addSeq(detectedPathKeyPrefix, seq));
        QingzhouMain.getBundleContext().getBundle(addSeq(bundleLocation, seq)).stop();
    }

    private void startInstance(String path, Set<Integer> used) {
        int seq = 0;
        while (used.contains(seq)) seq++;
        used.add(seq);
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
            System.err.println("App " + symbolicName + " failed to start, path: " + path + " : " + e.getMessage());
        }
    }

    private static String addSeq(String str, int seq) {
        return str + "[" + seq + "]";
    }
}
