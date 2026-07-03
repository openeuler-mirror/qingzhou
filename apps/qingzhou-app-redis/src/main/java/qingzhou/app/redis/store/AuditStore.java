package qingzhou.app.redis.store;

import qingzhou.api.AppContext;
import qingzhou.app.redis.store.model.AuditEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AuditStore extends AbstractStore<AuditEntry> {

    private static final int MAX_ENTRIES = 10000;

    public AuditStore(AppContext appContext) {
        super(appContext, StoreConstants.AUDIT_LOG_FILE);
    }

    @Override
    protected int getMaxSize() {
        return MAX_ENTRIES;
    }

    @Override
    protected void load() {
        loadAppendEntries(AuditEntry.class);
    }

    public void log(AuditEntry entry) {
        addEntry(entry);
        appendToFile(entry);
    }

    public List<AuditEntry> query(String keyword, String type, int pageNum, int pageSize) {
        List<AuditEntry> filtered = filter(keyword, type);
        return paginate(filtered, pageNum, pageSize);
    }

    public int total(String keyword, String type) {
        return filter(keyword, type).size();
    }

    private List<AuditEntry> filter(String keyword, String type) {
        String kw = keyword == null ? "" : keyword.toLowerCase();
        String tp = type == null ? "" : type;
        List<AuditEntry> result = new ArrayList<>();
        for (AuditEntry entry : entries) {
            if (!tp.isEmpty() && !tp.equals(entry.getOperationType())) {
                continue;
            }
            if (!kw.isEmpty()) {
                boolean match = containsIgnoreCase(entry.getOperator(), kw)
                        || containsIgnoreCase(entry.getTargetType(), kw)
                        || containsIgnoreCase(entry.getTargetId(), kw)
                        || containsIgnoreCase(entry.getDetail(), kw)
                        || containsIgnoreCase(entry.getInstanceName(), kw);
                if (!match) {
                    continue;
                }
            }
            result.add(entry);
        }
        Collections.reverse(result);
        return result;
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    public AuditEntry getById(String id) {
        return findById(id);
    }
}
