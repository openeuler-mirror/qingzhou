package qingzhou.app.redis;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import qingzhou.api.*;
import qingzhou.app.redis.alert.AlertEngine;
import qingzhou.app.redis.collector.CollectorScheduler;
import qingzhou.app.redis.collector.InstanceCollector;
import qingzhou.app.redis.collector.MachineCollector;
import qingzhou.app.redis.diagnosis.DiagnosticEngine;
import qingzhou.app.redis.service.MemoryAnalysis;
import qingzhou.app.redis.store.*;
import qingzhou.app.redis.store.model.AuditEntry;
import qingzhou.app.redis.util.RedisUtil;

@Menu(name = {"Redis 管理", "en:Redis Manager"}, code = "redis", icon = "DataStore", order = 1)
@Menu(name = {"运维管理", "en:Operations"}, code = "redis-ops", icon = "Setting", order = 4, parent = "redis")
@Menu(name = {"统计监控", "en:Monitoring"}, code = "redis-monitor", icon = "Monitor", order = 5, parent = "redis")

@I18n(name = {"Key 数量分布", "en:Key Count Distribution"}, code = "KeyCount")
@I18n(name = {"内存分布", "en:Memory Distribution"}, code = "MemoryDist")

@App(code = "redis", icon = "/icons/redis.svg",
        name = {"Redis管理", "en:Redis Manager"},
        info = {"Redis日常运维管理控制台，支持单机/哨兵/集群模式。",
                "en:Redis management console, supports standalone/sentinel/cluster modes."})
public class RedisApp implements QingzhouApp {

    private static volatile RedisUtil redisUtil;
    private static volatile String currentInstanceName;
    private static volatile String currentEnvType = "development";
    private static final Map<String, Map<String, String>> instances = new ConcurrentHashMap<>();
    private static volatile AppContext appContext;

    private static volatile MetricsStore metricsStore;
    private static volatile AuditStore auditStore;
    private static volatile AlertStore alertStore;
    private static volatile DiagnosisStore diagnosisStore;


    private static final String ENCRYPTED_PREFIX = "ENC:";

    public static String getEncryptedPrefix() {
        return ENCRYPTED_PREFIX;
    }

    @Override
    public void start(AppContext appContext) throws Exception {
        RedisApp.appContext = appContext;


        initStores(appContext);


        loadInstances(appContext);


        if (currentInstanceName != null && instances.containsKey(currentInstanceName)) {
            activateInstance(currentInstanceName);
        }


        appContext.addActionFilter((request, chain) -> {
            chain.doFilter();
            auditWriteAction(request);
        });


        startScheduler();
    }

    @Override
    public void stop() {
        CollectorScheduler.shutdownInstance();
        if (metricsStore != null) {
            metricsStore.saveSnapshot();
        }
        if (alertStore != null) {
            alertStore.saveSnapshot();
        }
        if (redisUtil != null) {
            redisUtil.close();
            redisUtil = null;
        }
    }

    private void initStores(AppContext appContext) {
        metricsStore = new MetricsStore(appContext);
        auditStore = new AuditStore(appContext);
        alertStore = new AlertStore(appContext);
        diagnosisStore = new DiagnosisStore(appContext);
    }

    private void startScheduler() {
        InstanceCollector instanceCollector = new InstanceCollector(metricsStore);
        MachineCollector machineCollector = new MachineCollector(metricsStore);
        AlertEngine.initialize(alertStore, metricsStore);
        DiagnosticEngine.initialize(diagnosisStore, metricsStore);
        CollectorScheduler.initialize(instanceCollector, machineCollector, AlertEngine.getInstance(), DiagnosticEngine.getInstance(), null);
    }

    private void auditWriteAction(Request request) {
        try {
            String action = request.getAction();
            String model = request.getModel();
            if (action == null || model == null) {
                return;
            }

            if (!isWriteAction(action)) {
                return;
            }
            AuditEntry entry = new AuditEntry();
            String operator = request.getParameter("operator");
            entry.setOperator(operator != null && !operator.isEmpty() ? operator : "unknown");
            entry.setOperationType(action);
            entry.setTargetType(model);
            entry.setTargetId(request.getId() != null ? request.getId() : "");
            entry.setInstanceName(currentInstanceName != null ? currentInstanceName : "");
            entry.setEnvType(currentEnvType);
            entry.setResult("成功");
            entry.setDetail(request.getParameter("detail"));
            auditStore.log(entry);
        } catch (Exception e) {
        }
    }

    private boolean isWriteAction(String action) {
        return "add".equals(action) || "update".equals(action) || "delete".equals(action)
                || "execute".equals(action) || "switch".equals(action) || "confirm".equals(action);
    }

    public static MetricsStore getMetricsStore() {
        return metricsStore;
    }

    public static AuditStore getAuditStore() {
        return auditStore;
    }

    public static AlertStore getAlertStore() {
        return alertStore;
    }

    public static DiagnosisStore getDiagnosisStore() {
        return diagnosisStore;
    }

    public static AppContext getAppContext() {
        return appContext;
    }

    public static RedisUtil getRedisUtil() {
        return redisUtil;
    }

    public static String getCurrentEnvType() {
        return currentEnvType;
    }

    public static String getCurrentInstanceName() {
        return currentInstanceName;
    }

    public static Map<String, Map<String, String>> getInstances() {
        return instances;
    }

    public static synchronized void activateInstance(String id) throws Exception {

        String name = null;
        Map<String, String> config = null;
        for (Map.Entry<String, Map<String, String>> entry : instances.entrySet()) {
            if (id.equals(entry.getValue().get("id")) || id.equals(entry.getKey())) {
                name = entry.getKey();
                config = entry.getValue();
                break;
            }
        }
        if (config == null) throw new Exception("实例不存在: " + id);


        RedisUtil newUtil = new RedisUtil();
        Map<String, String> decryptedConfig = new LinkedHashMap<>(config);
        String encryptedPassword = decryptedConfig.getOrDefault("password", "");
        if (encryptedPassword.startsWith(ENCRYPTED_PREFIX)) {
            try {
                String decrypted = decryptPassword(encryptedPassword.substring(ENCRYPTED_PREFIX.length()));
                decryptedConfig.put("password", decrypted);
            } catch (Exception e) {
                throw new Exception("密码解密失败: " + e.getMessage());
            }
        }
        try {
            newUtil.init(decryptedConfig);
        } catch (Exception e) {
            throw new Exception("连接失败: " + e.getMessage(), e);
        }


        RedisUtil oldUtil = redisUtil;
        redisUtil = newUtil;
        currentInstanceName = name;
        currentEnvType = config.getOrDefault("envType", "development");
        if (oldUtil != null) {
            try {
                oldUtil.close();
            } catch (Exception ignored) {
            }
        }


        MemoryAnalysis.clearCache();
    }

    public static synchronized void deactivateInstance() {
        if (redisUtil != null) {
            redisUtil.close();
            redisUtil = null;
        }
        currentInstanceName = null;
        currentEnvType = "development";
        MemoryAnalysis.clearCache();
    }




    private static String decryptPassword(String encrypted) {
        return new String(Base64.getDecoder().decode(encrypted));
    }

    private void loadInstances(AppContext appContext) {
        Properties props = appContext.getProperties();
        if (props == null) {
            return;
        }

        Map<String, Map<String, String>> loaded = new LinkedHashMap<>();
        String activeIndex = null;


        for (String key : new TreeSet<>(props.stringPropertyNames())) {
            String value = props.getProperty(key);
            if (value == null) {
                continue;
            }


            int dot = key.indexOf('.');
            if (dot <= 0 || dot == key.length() - 1) {
                continue;
            }

            String index = key.substring(0, dot);
            String property = key.substring(dot + 1);

            Map<String, String> config = loaded.computeIfAbsent(index, k -> {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("id", k);
                return m;
            });
            config.put(property, value);

            if ("active".equals(property) && Boolean.parseBoolean(value)) {
                activeIndex = index;
            }
        }

        for (Map.Entry<String, Map<String, String>> entry : loaded.entrySet()) {
            String index = entry.getKey();
            Map<String, String> config = entry.getValue();
            String name = config.getOrDefault("name", index);


            config.put("id", index);
            config.putIfAbsent("name", name);
            config.putIfAbsent("mode", "standalone");
            config.putIfAbsent("host", "127.0.0.1");
            config.putIfAbsent("port", "6379");
            config.putIfAbsent("database", "0");
            config.putIfAbsent("envType", "development");
            config.putIfAbsent("sentinelMaster", "mymaster");

            instances.put(name, config);
        }

        if (activeIndex != null) {
            for (Map.Entry<String, Map<String, String>> entry : instances.entrySet()) {
                if (activeIndex.equals(entry.getValue().get("id"))) {
                    currentInstanceName = entry.getKey();
                    break;
                }
            }
        }
    }
}
