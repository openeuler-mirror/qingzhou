package qingzhou.app.redis.store;

import qingzhou.api.AppContext;
import qingzhou.app.redis.store.model.DiagnosisReport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

public class DiagnosisStore extends AbstractStore<DiagnosisReport> {

    private static final int MAX_REPORTS = 100;

    public DiagnosisStore(AppContext appContext) {
        super(appContext, StoreConstants.DIAGNOSIS_FILE);
    }

    @Override
    protected int getMaxSize() {
        return MAX_REPORTS;
    }

    @Override
    protected void load() {
        Snapshot snapshot = loadSnapshot(Snapshot.class);
        if (snapshot != null) {
            entries.clear();
            if (snapshot.getReports() != null) {
                for (DiagnosisReport report : snapshot.getReports()) {
                    entries.offerLast(report);
                }
            }
            sequence.set(snapshot.getSequence());
        }
    }

    public void add(DiagnosisReport report) {
        addEntry(report);
        saveSnapshot(buildSnapshot());
    }

    public List<DiagnosisReport> query(String category, int pageNum, int pageSize) {
        List<DiagnosisReport> filtered = new ArrayList<>();
        for (DiagnosisReport report : entries) {
            if (category != null && !category.isEmpty() && !category.equals(report.getCategory())) {
                continue;
            }
            filtered.add(report);
        }
        Collections.reverse(filtered);
        return paginate(filtered, pageNum, pageSize);
    }

    public int total(String category) {
        int count = 0;
        for (DiagnosisReport report : entries) {
            if (category == null || category.isEmpty() || category.equals(report.getCategory())) {
                count++;
            }
        }
        return count;
    }

    public DiagnosisReport getById(String id) {
        return findById(id);
    }

    public void clear() {
        entries.clear();
        saveSnapshot(buildSnapshot());
    }

    public Deque<DiagnosisReport> getReports() {
        return entries;
    }

    private Snapshot buildSnapshot() {
        Snapshot snapshot = new Snapshot();
        snapshot.setReports(new ArrayList<>(entries));
        snapshot.setSequence(sequence.get());
        return snapshot;
    }

    public static class Snapshot {
        private List<DiagnosisReport> reports = new ArrayList<>();
        private long sequence;

        public List<DiagnosisReport> getReports() {
            return reports;
        }

        public void setReports(List<DiagnosisReport> reports) {
            this.reports = reports;
        }

        public long getSequence() {
            return sequence;
        }

        public void setSequence(long sequence) {
            this.sequence = sequence;
        }
    }
}
