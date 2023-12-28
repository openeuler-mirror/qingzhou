package qingzhou.console.audit;

import java.util.List;
import java.util.Map;

public interface AuditInterface {
    void write(AuditFilter.LogLine logLine);

    void reload();

    SearchResult read(int pageSize, int pageNum, Map<String, String> filterParams, AuditFilter.Cache cache) throws Exception;

    class SearchResult {
        private final long totalLine;
        private final List<String[]> pageResult;

        public SearchResult(long totalLine, List<String[]> pageResult) {
            this.totalLine = totalLine;
            this.pageResult = pageResult;
        }

        public long getTotalLine() {
            return totalLine;
        }

        public List<String[]> getPageResult() {
            return pageResult;
        }
    }
}
