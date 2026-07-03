package qingzhou.app.redis.store;

import qingzhou.api.AppContext;
import qingzhou.app.redis.store.model.AlertRecord;
import qingzhou.app.redis.store.model.AlertRule;
import qingzhou.json.Json;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

public class AlertStore {

    private static final int MAX_RECORDS = 5000;
    private final List<AlertRule> rules = Collections.synchronizedList(new ArrayList<>());
    private final Deque<AlertRecord> records = new ConcurrentLinkedDeque<>();
    private final AppContext appContext;
    private final Path storageFile;
    private final AtomicLong ruleSequence = new AtomicLong(0);
    private final AtomicLong recordSequence = new AtomicLong(0);

    public AlertStore(AppContext appContext) {
        this.appContext = appContext;
        this.storageFile = getStorageFile();
        loadSnapshot();
    }

    private Path getStorageFile() {
        File base = appContext.getBase();
        File dir = new File(base, StoreConstants.DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, StoreConstants.ALERTS_FILE).toPath();
    }

    

    public List<AlertRule> listRules() {
        synchronized (rules) {
            return new ArrayList<>(rules);
        }
    }

    public AlertRule getRule(String id) {
        synchronized (rules) {
            for (AlertRule rule : rules) {
                if (id != null && id.equals(rule.getId())) {
                    return rule;
                }
            }
            return null;
        }
    }

    public void addRule(AlertRule rule) {
        if (rule == null) {
            return;
        }
        if (rule.getId() == null) {
            rule.setId(String.valueOf(ruleSequence.incrementAndGet()));
        }
        if (rule.getCreatedAt() <= 0) {
            rule.setCreatedAt(System.currentTimeMillis());
        }
        synchronized (rules) {
            rules.add(rule);
        }
        saveSnapshot();
    }

    public void updateRule(AlertRule rule) {
        if (rule == null || rule.getId() == null) {
            return;
        }
        synchronized (rules) {
            for (int i = 0; i < rules.size(); i++) {
                if (rule.getId().equals(rules.get(i).getId())) {
                    rules.set(i, rule);
                    break;
                }
            }
        }
        saveSnapshot();
    }

    public void deleteRule(String id) {
        synchronized (rules) {
            rules.removeIf(rule -> id != null && id.equals(rule.getId()));
        }
        saveSnapshot();
    }

    

    public void addRecord(AlertRecord record) {
        if (record == null) {
            return;
        }
        if (record.getId() == null) {
            record.setId(String.valueOf(recordSequence.incrementAndGet()));
        }
        if (record.getTriggeredAt() <= 0) {
            record.setTriggeredAt(System.currentTimeMillis());
        }
        records.offerLast(record);
        while (records.size() > MAX_RECORDS) {
            records.pollFirst();
        }
        saveSnapshot();
    }

    public List<AlertRecord> queryRecords(String status, int pageNum, int pageSize) {
        List<AlertRecord> filtered = new ArrayList<>();
        for (AlertRecord record : records) {
            if (status != null && !status.isEmpty() && !status.equals(record.getStatus())) {
                continue;
            }
            filtered.add(record);
        }
        Collections.reverse(filtered);
        int start = (pageNum - 1) * pageSize;
        if (start >= filtered.size()) {
            return new ArrayList<>();
        }
        int end = Math.min(start + pageSize, filtered.size());
        return filtered.subList(start, end);
    }

    public int totalRecords(String status) {
        int count = 0;
        for (AlertRecord record : records) {
            if (status == null || status.isEmpty() || status.equals(record.getStatus())) {
                count++;
            }
        }
        return count;
    }

    public AlertRecord getRecord(String id) {
        if (id == null) {
            return null;
        }
        for (AlertRecord record : records) {
            if (id.equals(record.getId())) {
                return record;
            }
        }
        return null;
    }

    public void confirmRecord(String id, String operator) {
        AlertRecord record = getRecord(id);
        if (record == null) {
            return;
        }
        record.setStatus("已确认");
        record.setConfirmedBy(operator);
        record.setConfirmedAt(System.currentTimeMillis());
        saveSnapshot();
    }

    public Deque<AlertRecord> getRecords() {
        return records;
    }

    

    public synchronized void saveSnapshot() {
        try {
            Json json = appContext.getService(Json.class);
            if (json == null) {
                return;
            }
            Snapshot snapshot = new Snapshot();
            synchronized (rules) {
                snapshot.setRules(new ArrayList<>(rules));
            }
            snapshot.setRecords(new ArrayList<>(records));
            snapshot.setRuleSequence(ruleSequence.get());
            snapshot.setRecordSequence(recordSequence.get());
            String data = json.toJson(snapshot);
            Files.write(storageFile, data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
        }
    }

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
            if (snapshot != null) {
                rules.clear();
                if (snapshot.getRules() != null) {
                    rules.addAll(snapshot.getRules());
                }
                records.clear();
                if (snapshot.getRecords() != null) {
                    records.addAll(snapshot.getRecords());
                }
                ruleSequence.set(snapshot.getRuleSequence());
                recordSequence.set(snapshot.getRecordSequence());
            }
        } catch (Exception e) {
        }
    }

    public static class Snapshot {
        private List<AlertRule> rules = new ArrayList<>();
        private List<AlertRecord> records = new ArrayList<>();
        private long ruleSequence;
        private long recordSequence;

        public List<AlertRule> getRules() {
            return rules;
        }

        public void setRules(List<AlertRule> rules) {
            this.rules = rules;
        }

        public List<AlertRecord> getRecords() {
            return records;
        }

        public void setRecords(List<AlertRecord> records) {
            this.records = records;
        }

        public long getRuleSequence() {
            return ruleSequence;
        }

        public void setRuleSequence(long ruleSequence) {
            this.ruleSequence = ruleSequence;
        }

        public long getRecordSequence() {
            return recordSequence;
        }

        public void setRecordSequence(long recordSequence) {
            this.recordSequence = recordSequence;
        }
    }
}
