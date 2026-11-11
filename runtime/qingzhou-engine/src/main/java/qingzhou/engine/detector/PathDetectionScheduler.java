package qingzhou.engine.detector;

import org.osgi.framework.Bundle;
import qingzhou.engine.QingzhouMain;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PathDetectionScheduler {
    private static final String DETECTOR_NAME = "qz-path-detector";
    private static ScheduledExecutorService scheduledExecutor;
    public static final Map<String, List<String>> detectedMap = new HashMap<>();

    public static void startWithDetection(Bundle bundle) {
        if (scheduledExecutor == null) {
            scheduledExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, DETECTOR_NAME);
                thread.setDaemon(true);
                return thread;
            });
        }

        PathDetector detector = new PathDetector(bundle.getHeaders());
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try { // 实际检测逻辑
                List<String> detectedList = detector.detect();
                if (!detectedList.isEmpty()) {
                    List<String> historyDetectedList = detectedMap.computeIfAbsent(bundle.getSymbolicName(), k -> new ArrayList<>(4));
                    // 停止未检测到的应用实例
                    for (String detected : historyDetectedList) {
                        if (!detectedList.contains(detected)) {
                            int i = historyDetectedList.indexOf(detected);
                            String dataFlag = addSeq("Qingzhou-Detected-Path." + bundle.getSymbolicName(), i);
                            System.clearProperty(dataFlag);
                            Bundle bnd = QingzhouMain.getBundleContext().getBundle(addSeq(bundle.getLocation(), i));
                            if (bnd != null) {
                                bnd.stop();
                            }
                        }
                    }
                    // 启动已检测到的实例
                    for (String detected : detectedList) {
                        if (!historyDetectedList.contains(detected)) {
                            historyDetectedList.add(detected);
                        }
                        int i = historyDetectedList.indexOf(detected);
                        String dataFlag = addSeq("Qingzhou-Detected-Path." + bundle.getSymbolicName(), i);
                        System.setProperty(dataFlag, detected);
                        try (InputStream inputStream = Files.newInputStream(new File(new URL(bundle.getLocation()).getFile()).toPath())) {
                            Bundle installed = QingzhouMain.getBundleContext().installBundle(addSeq(bundle.getLocation(), i), inputStream);
                            installed.start();
                        } catch (Exception e) {
                            System.err.println("App " + bundle.getSymbolicName() + " failed to start, path: " + detected + " : " + e.getMessage());
                        }
                    }
                } else {
                    for (Bundle bnd : QingzhouMain.getBundleContext().getBundles()) {
                        if (bnd.getLocation().startsWith(bundle.getLocation())) {
                            bnd.stop();
                        }
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
        return String.format("%s[%d]", str, seq);
    }
}
