package qingzhou.console.audit.impl;

import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.console.audit.AuditFilter;
import qingzhou.console.audit.AuditInterface;
import qingzhou.console.util.FileUtil;
import qingzhou.console.util.StringUtil;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TextAudit implements AuditInterface {
    private final String SP = ",";
    private final String cacheKey = "CacheKey:" + TextAudit.class.getName();
    private File logFile;

    public TextAudit() {
        reload();
    }

    @Override
    public void reload() {
        String loggerName = "audit";
        logFile = FileUtil.newFile(ConsoleWarHelper.getLogs(), loggerName, loggerName + ".log");
    }

    @Override
    public void write(AuditFilter.LogLine logLine) {
        StringBuilder dataLine = new StringBuilder();
        boolean notFirst = false;
        for (AuditFilter.LogField lf : AuditFilter.LogField.values()) {
            if (notFirst) {
                dataLine.append(SP);
            }
            dataLine.append(logLine.get(lf));
            notFirst = true;
        }
        String log = dataLine.toString();
        ConsoleWarHelper.getLogger().info(log);
    }

    @Override
    public SearchResult read(int pageSize, int pageNum, Map<String, String> filterParams, AuditFilter.Cache cache) throws Exception {
        if (!logFile.exists()) {
            return new SearchResult(0, null);
        }

        // 目标
        int totalLines;
        List<String[]> result = null;

        LineNumberConverter converter;
        if (filterParams.isEmpty()) {
            String cacheId = (String) cache.getCache(cacheKey);
            cache.setCache(cacheKey, null);
            if (cacheId != null) {
                cache.setCache(cacheId, null);
            }

            // 总行数
            totalLines = FileUtil.fileTotalLines(logFile);
            converter = lineNumber -> lineNumber;
        } else {
            String paramsId = retrieveId(filterParams);
            List<Integer> lineCache = (List<Integer>) cache.getCache(paramsId);
            if (lineCache == null) {
                // init cache
                lineCache = new ArrayList<>();
                try (LineNumberReader reader = new LineNumberReader(new FileReader(logFile))) {
                    for (String line; (line = reader.readLine()) != null; ) {
                        if (matches(line, filterParams)) {
                            lineCache.add(reader.getLineNumber());
                        }
                    }
                }
                // 更新缓存
                String cacheId = (String) cache.getCache(cacheKey);
                cache.setCache(cacheKey, paramsId);// 覆盖原来的缓存
                if (cacheId != null) {
                    cache.setCache(cacheId, null);// 清楚原来的缓存
                    cache.setCache(paramsId, lineCache);// 添加新的缓存
                }
            }

            // 获取指定页的数据
            totalLines = lineCache.size();
            List<Integer> finalLineCache = lineCache;
            converter = lineNumber -> finalLineCache.get(lineNumber - 1);
        }

        int begin = totalLines - pageNum * pageSize + 1;
        int end = begin + pageSize - 1;
        if (begin < 1) {
            begin = 1;
        }
        if (end > totalLines) {
            end = totalLines;
        }
        int len = end - begin + 1;
        if (len > 0) {
            result = new ArrayList<>();
            long[] resultLineNumbers = new long[len];
            for (int i = 0; i < len; i++) {
                resultLineNumbers[i] = converter.convert(begin + i);
            }
            try (LineNumberReader reader = new LineNumberReader(new FileReader(logFile))) {
                for (String line; (line = reader.readLine()) != null; ) {
                    int lineNumber = reader.getLineNumber();
                    boolean found = false;
                    for (long check : resultLineNumbers) {
                        if (check == lineNumber) {
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        result.add(line.split(SP));
                    }
                    if (lineNumber > resultLineNumbers[resultLineNumbers.length - 1]) {
                        break;
                    }
                }
            }
            Collections.reverse(result);// 反转顺序，最新的数据放在最前
        }
        return new SearchResult(totalLines, result);
    }

    private String retrieveId(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        TreeMap<String, String> treeMap = new TreeMap<>(map);
        StringBuilder id = new StringBuilder();
        for (Map.Entry<String, String> e : treeMap.entrySet()) {
            id.append(e.getKey()).append(e.getValue());
        }
        return id.toString();
    }

    private boolean matches(String logLine, Map<String, String> filterParams) {
        if (StringUtil.isBlank(logLine) || filterParams.isEmpty()) {
            return false;
        }

        String[] logItems = logLine.split(SP);
        String key;
        for (Map.Entry<String, String> param : filterParams.entrySet()) {
            key = param.getKey();
            AuditFilter.LogField lf = AuditFilter.LogField.valueOf(key);
            if (!logItems[lf.ordinal()].contains(param.getValue())) {
                return false;
            }
        }

        return true;
    }

    interface LineNumberConverter {
        int convert(int lineNumber);
    }
}
