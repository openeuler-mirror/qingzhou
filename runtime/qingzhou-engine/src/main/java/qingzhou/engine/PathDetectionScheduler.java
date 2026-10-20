package qingzhou.engine;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.Bundle;

public class PathDetectionScheduler {
    private static ScheduledExecutorService scheduledExecutor;

    static void startWithDetection(Bundle bundle) {
        if (scheduledExecutor == null) {
            scheduledExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "qz-path-detector");
                thread.setDaemon(true);
                return thread;
            });
        }
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try { // 实际检测逻辑
                PathDetector detector = new PathDetector(bundle.getHeaders());
                String detected = detector.detect();
                if (detected != null) {
                    // 按 bundle 符号名分键传递，避免多个探测型 bundle 互相覆盖，
                    // 也避免"设置后即清除"与 Activator 异步读取之间的竞态；属性值随检测覆盖更新，无需清除
                    String dataFlag = "Qingzhou-Detected-Path." + bundle.getSymbolicName();
                    System.setProperty(dataFlag, detected);
                    bundle.start();
                } else {
                    bundle.stop();
                }
            } catch (Exception e) {
                // 捕获并打印，保证任务不被取消；引导模块先于日志体系启动，只能直写 stderr
                System.err.println("qz-path-detector task error: " + e.getMessage());
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    static void shutdown() {
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdownNow();
        }
    }
}
