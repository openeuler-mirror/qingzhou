package qingzhou.app.redis.store;

import qingzhou.api.AppContext;
import qingzhou.app.redis.store.model.MetricPoint;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class MetricsStore {

    private static final int MAX_POINTS_PER_METRIC = 1440;
    private final Map<String, Deque<MetricPoint>> metrics = new ConcurrentHashMap<>();
    private final SnapshotFile snapshotFile;
    private volatile long lastSnapshotTime = 0;
    private static final long SNAPSHOT_INTERVAL_MS = 5 * 60 * 1000;

    public MetricsStore(AppContext appContext) {
        this.snapshotFile = new SnapshotFile(appContext, StoreConstants.METRICS_FILE);
        loadSnapshot();
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
        Snapshot snapshot = new Snapshot();
        snapshot.setMetrics(metrics);
        snapshot.setLastSnapshotTime(System.currentTimeMillis());
        snapshotFile.save(snapshot);
    }

    @SuppressWarnings("unchecked")
    private synchronized void loadSnapshot() {
        Snapshot snapshot = snapshotFile.load(Snapshot.class);
        if (snapshot == null || snapshot.getMetrics() == null) return;
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
