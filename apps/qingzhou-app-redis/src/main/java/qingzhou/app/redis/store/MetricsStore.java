package qingzhou.app.redis.store;

import qingzhou.api.AppContext;
import qingzhou.app.redis.store.model.MetricPoint;
import qingzhou.json.Json;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class MetricsStore {

    private static final int MAX_POINTS_PER_METRIC = 1440;
    private final Map<String, Deque<MetricPoint>> metrics = new ConcurrentHashMap<>();
    private final AppContext appContext;
    private final Path storageFile;
    private volatile long lastSnapshotTime = 0;
    private static final long SNAPSHOT_INTERVAL_MS = 5 * 60 * 1000;

    public MetricsStore(AppContext appContext) {
        this.appContext = appContext;
        this.storageFile = getStorageFile();
        loadSnapshot();
    }

    private Path getStorageFile() {
        File base = appContext.getRoot();
        File dir = new File(base, StoreConstants.DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, StoreConstants.METRICS_FILE).toPath();
    }

    public void record(String metric, double value) {
        record(metric, value, System.currentTimeMillis());
    }

    public void record(String metric, double value, long timestamp) {
        if (metric == null || metric.isEmpty()) {
            return;
        }
        Deque<MetricPoint> deque = metrics.computeIfAbsent(metric, k -> new ConcurrentLinkedDeque<>());
        deque.offerLast(new MetricPoint(timestamp, value));
        while (deque.size() > MAX_POINTS_PER_METRIC) {
            deque.pollFirst();
        }
        maybeSnapshot();
    }

    public List<MetricPoint> query(String metric, long start, long end) {
        Deque<MetricPoint> deque = metrics.get(metric);
        if (deque == null) {
            return new ArrayList<>();
        }
        List<MetricPoint> result = new ArrayList<>();
        for (MetricPoint point : deque) {
            if (point.getTimestamp() >= start && point.getTimestamp() <= end) {
                result.add(point);
            }
        }
        return result;
    }

    public Double getLatest(String metric) {
        Deque<MetricPoint> deque = metrics.get(metric);
        if (deque == null || deque.isEmpty()) {
            return null;
        }
        return deque.peekLast().getValue();
    }

    public Map<String, List<MetricPoint>> queryLatest(int points) {
        Map<String, List<MetricPoint>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Deque<MetricPoint>> entry : metrics.entrySet()) {
            Deque<MetricPoint> deque = entry.getValue();
            List<MetricPoint> list = new ArrayList<>(deque);
            if (list.size() > points) {
                list = list.subList(list.size() - points, list.size());
            }
            result.put(entry.getKey(), list);
        }
        return result;
    }

    public Map<String, Deque<MetricPoint>> getAllMetrics() {
        return new LinkedHashMap<>(metrics);
    }

    public Set<String> getMetricNames() {
        return new LinkedHashSet<>(metrics.keySet());
    }

    private void maybeSnapshot() {
        long now = System.currentTimeMillis();
        if (now - lastSnapshotTime < SNAPSHOT_INTERVAL_MS) {
            return;
        }
        synchronized (this) {
            if (now - lastSnapshotTime < SNAPSHOT_INTERVAL_MS) {
                return;
            }
            saveSnapshot();
            lastSnapshotTime = now;
        }
    }

    public synchronized void saveSnapshot() {
        try {
            Json json = appContext.getService(Json.class);
            if (json == null) {
                return;
            }
            Snapshot snapshot = new Snapshot();
            snapshot.setMetrics(metrics);
            snapshot.setLastSnapshotTime(System.currentTimeMillis());
            String data = json.toJson(snapshot);
            Files.write(storageFile, data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private synchronized void loadSnapshot() {
        if (!Files.exists(storageFile)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(storageFile, StandardCharsets.UTF_8)) {
            Json json = appContext.getService(Json.class);
            if (json == null) {
                return;
            }
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            Snapshot snapshot = json.fromJson(sb.toString(), Snapshot.class);
            if (snapshot != null && snapshot.getMetrics() != null) {
                metrics.clear();
                for (Map.Entry<String, Deque<MetricPoint>> entry : snapshot.getMetrics().entrySet()) {
                    Deque<MetricPoint> deque = new ConcurrentLinkedDeque<>();
                    for (MetricPoint point : entry.getValue()) {
                        deque.offerLast(point);
                    }
                    while (deque.size() > MAX_POINTS_PER_METRIC) {
                        deque.pollFirst();
                    }
                    metrics.put(entry.getKey(), deque);
                }
                lastSnapshotTime = snapshot.getLastSnapshotTime();
            }
        } catch (Exception ignored) {
        }
    }

    public static class Snapshot {
        private Map<String, Deque<MetricPoint>> metrics = new LinkedHashMap<>();
        private long lastSnapshotTime;

        public Map<String, Deque<MetricPoint>> getMetrics() {
            return metrics;
        }

        public void setMetrics(Map<String, Deque<MetricPoint>> metrics) {
            this.metrics = metrics;
        }

        public long getLastSnapshotTime() {
            return lastSnapshotTime;
        }

        public void setLastSnapshotTime(long lastSnapshotTime) {
            this.lastSnapshotTime = lastSnapshotTime;
        }
    }
}
