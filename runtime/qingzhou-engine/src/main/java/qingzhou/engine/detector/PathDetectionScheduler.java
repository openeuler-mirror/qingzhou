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
import qingzhou.engine.QingzhouMain;

public class PathDetectionScheduler {
    private static final String DETECTOR_NAME = "qz-path-detector";
    private static ScheduledExecutorService scheduledExecutor;

    public static void startWithDetection(Bundle bundle, String instanceDir) {
        if (scheduledExecutor == null) {
            scheduledExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, DETECTOR_NAME);
                thread.setDaemon(true);
                return thread;
            });
        }

        PathDetector detector = new PathDetector(bundle.getHeaders());
        String symbolicName = bundle.getSymbolicName();
        String location = bundle.getLocation();
        String pathFlagPrefix = "Qingzhou-Detected-Path." + symbolicName;
        // location 由 toURI() 生成（见 QingzhouMain），须按 URI 解码，否则含空格/中文的路径读不到
        Path bundleJar = Paths.get(URI.create(location));
        PathSeqAllocator seqAllocator;
        try {
            seqAllocator = new PathSeqAllocator(instanceDir, symbolicName);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load detected path seq of " + symbolicName + " : " + e.getMessage(), e);
        }

        // 本进程内已启动的 路径→序号；首拍必为空，故上个进程遗留在 framework 缓存里的同 location bundle 会被重装，刷成路径上的最新 jar
        Map<String, Integer> running = new HashMap<>();
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                Map<String, Integer> detected = seqAllocator.assign(detector.detect());

                for (Map.Entry<String, Integer> entry : new HashMap<>(running).entrySet()) {
                    if (detected.containsKey(entry.getKey())) continue;
                    System.clearProperty(addSeq(pathFlagPrefix, entry.getValue()));
                    Bundle stale = QingzhouMain.getBundleContext().getBundle(addSeq(location, entry.getValue()));
                    if (stale != null) stale.stop();
                    running.remove(entry.getKey());
                }

                for (Map.Entry<String, Integer> entry : detected.entrySet()) {
                    String path = entry.getKey();
                    if (running.containsKey(path)) continue;
                    int seq = entry.getValue();
                    String seqLocation = addSeq(location, seq);
                    System.setProperty(addSeq(pathFlagPrefix, seq), path);
                    BundleContext bundleContext = QingzhouMain.getBundleContext();
                    try (InputStream inputStream = Files.newInputStream(bundleJar)) {
                        // 同 location 的 bundle 可能来自上个进程或已回收的旧路径，其内容未必对应当前路径，须卸载后重装
                        Bundle existing = bundleContext.getBundle(seqLocation);
                        if (existing != null) existing.uninstall();
                        bundleContext.installBundle(seqLocation, inputStream).start();
                        running.put(path, seq);
                    } catch (Exception e) {
                        System.err.println("App " + symbolicName + " failed to start, path: " + path + " : " + e.getMessage());
                    }
                }

                if (detected.isEmpty()) {
                    for (Bundle bnd : QingzhouMain.getBundleContext().getBundles()) {
                        if (bnd.getLocation().startsWith(location)) bnd.stop();
                    }
                }
            } catch (Exception e) {
                // 捕获并打印，保证任务不被取消；引导模块先于日志体系启动，只能直写 stderr
                System.err.println(DETECTOR_NAME + " task error: " + e.getMessage());
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    public static void shutdown() {
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdownNow();
        }
    }

    private static String addSeq(String str, int seq) {
        return str + "[" + seq + "]";
    }
}
