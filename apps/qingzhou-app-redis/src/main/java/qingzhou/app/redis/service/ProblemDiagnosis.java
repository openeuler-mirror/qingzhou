package qingzhou.app.redis.service;

import qingzhou.api.*;
import qingzhou.api.type.List;
import qingzhou.api.type.Show;
import qingzhou.app.redis.RedisApp;
import qingzhou.app.redis.RedisModelBase;
import qingzhou.app.redis.diagnosis.DiagnosticEngine;
import qingzhou.app.redis.store.model.DiagnosisReport;
import qingzhou.app.redis.util.RedisUtil;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Model(code = "problemDiagnosis", icon = "FirstAidKit", menu = "redis-ops", order = 3,
        name = {"诊断分析", "en:Diagnostic Analysis"},
        info = {"Redis 问题综合诊断报告", "en:Comprehensive Redis problem diagnosis reports"})
public class ProblemDiagnosis extends RedisModelBase implements List, Show {

    @ModelField(id = true, list = true, show = true, readonly = true,
            name = {"报告 ID", "en:Report ID"},
            info = {"诊断报告唯一标识", "en:Unique report ID"})
    public String id;

    @ModelField(list = true, show = true,
            name = {"诊断时间", "en:Diagnosis Time"},
            info = {"诊断发生时间", "en:Diagnosis time"})
    public String diagnosisTime;

    @ModelField(list = true, show = true, search = true,
            options = {"性能", "内存", "连接", "配置"},
            input_type = InputType.select,
            name = {"问题分类", "en:Category"},
            info = {"问题分类", "en:Problem category"})
    public String category;

    @ModelField(list = true, show = true,
            name = {"诊断项名称", "en:Diagnosis Name"},
            info = {"诊断项名称", "en:Diagnosis name"})
    public String diagnosisName;

    @ModelField(list = true, show = true,
            name = {"问题描述", "en:Description"},
            info = {"问题描述", "en:Problem description"})
    public String description;

    @ModelField(show = true,
            name = {"根因", "en:Root Cause"},
            info = {"问题根因", "en:Root cause"})
    public String cause;

    @ModelField(show = true,
            name = {"建议操作", "en:Suggestion"},
            info = {"建议操作", "en:Suggested operation"})
    public String suggestion;

    @ModelField(list = true, show = true,
            color = {"严重:#F56C6C", "警告:#E6A23C", "正常:#67C23A"},
            name = {"级别", "en:Level"},
            info = {"问题级别", "en:Problem level"})
    public String level;

    @ModelField(list = true, show = true,
            color = {"未处理:#F56C6C", "已处理:#67C23A"},
            name = {"状态", "en:Status"},
            info = {"处理状态", "en:Handling status"})
    public String status;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public java.util.List<String[]> list(int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception {
        String category = query != null ? query.get("category") : null;
        java.util.List<DiagnosisReport> reports = RedisApp.getDiagnosisStore().query(category, pageNum, pageSize);
        java.util.List<String[]> rows = new ArrayList<>();
        for (DiagnosisReport report : reports) {
            rows.add(toArray(report));
        }
        return rows;
    }

    @Override
    public int totalSize(Map<String, String> query) {
        String category = query != null ? query.get("category") : null;
        return RedisApp.getDiagnosisStore().total(category);
    }

    @Override
    public boolean contains(String id) {
        return RedisApp.getDiagnosisStore().getById(id) != null;
    }

    @Override
    public Map<String, String> show(String id) throws Exception {
        DiagnosisReport report = RedisApp.getDiagnosisStore().getById(id);
        if (report == null) {
            throw new Exception("诊断报告不存在: " + id);
        }
        return toMap(report);
    }

    @ModelAction(name = {"刷新诊断", "en:Refresh Diagnosis"},
            info = {"立即重新执行诊断分析", "en:Run diagnostic analysis immediately"},
            list_head = true)
    public void refresh(String id) throws Exception {
        RedisUtil util = getRedisUtil();
        DiagnosticEngine engine = DiagnosticEngine.getInstance();
        if (engine != null) {
            engine.diagnose(util);
        }
    }

    private String[] toArray(DiagnosisReport report) {
        return new String[]{
                report.getId(),
                report.getTimestamp() > 0 ? Instant.ofEpochMilli(report.getTimestamp()).atZone(ZoneId.systemDefault()).format(DATE_FORMAT) : "-",
                report.getCategory(),
                report.getTitle(),
                report.getDescription(),
                report.getLevel(),
                report.getStatus()
        };
    }

    private Map<String, String> toMap(DiagnosisReport report) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("id", report.getId());
        data.put("diagnosisTime", report.getTimestamp() > 0 ? Instant.ofEpochMilli(report.getTimestamp()).atZone(ZoneId.systemDefault()).format(DATE_FORMAT) : "-");
        data.put("category", report.getCategory());
        data.put("diagnosisName", report.getTitle());
        data.put("description", report.getDescription());
        data.put("cause", report.getCause());
        data.put("suggestion", report.getSuggestion());
        data.put("level", report.getLevel());
        data.put("status", report.getStatus());
        return data;
    }
}
