# Qingzhou 监控数据接入 Prometheus 指导手册

## 一、抓取端点

Qingzhou 管控台通过 `qingzhou-monitor` 模块暴露 Prometheus 标准抓取端点：

| 项 | 值 |
|----|----|
| 端点 | `/monitor/prometheus`（内部声明路径 `/prometheus`，按 bundle 前缀自动映射） |
| 协议 | OpenMetrics / Prometheus 文本格式（`Content-Type: text/plain; version=0.0.4; charset=utf-8`） |

访问示例：

```bash
curl -s http://<管控台地址>:7900/monitor/prometheus
```

## 二、指标命名与标签规范

指标名统一前缀 `qingzhou_`，后接字段编码的 snake_case 形式：

- 命名转换：`heapUsed` → `qingzhou_heap_used`，`cpuProcessUsage` → `qingzhou_cpu_process_usage`
- 保留后缀剥离：`threadCount` → `qingzhou_thread`、`slowLogTotal` → `qingzhou_slow_log`
- `# HELP` 文本取自 `AppMeta` 中字段的 `name` 属性；`# TYPE` 统一为 `gauge`

每个指标强制携带四维标签：

| 标签 | 含义 |
|------|------|
| `instance` | 实例标识（本地为 `-`，远程为 Heartbeat 生成的实例 ID） |
| `app` | 应用编码，如 `qingzhou-app-demo` |
| `model` | 模型编码，如 `jvm`、`os` |
| `field` | 字段原始编码，如 `heapUsed` |

示例：

```
# HELP qingzhou_heap_used 堆内存使用(MB)
# TYPE qingzhou_heap_used gauge
qingzhou_heap_used{instance="-",app="qingzhou-app-demo",model="jvm",field="heapUsed"} 72.0
```

## 三、Prometheus 抓取配置

在 `prometheus.yml` 中新增 scrape job：

```yaml
scrape_configs:
  - job_name: 'qingzhou'
    metrics_path: '/monitor/prometheus'
    static_configs:
      - targets: ['<管控台地址>:7900']
    scrape_interval: 15s
    scrape_timeout: 10s
```

多管控台场景可改为基于文件的服务发现（`file_sd_configs`）批量接入。

## 四、Grafana PromQL 示例

| 场景 | PromQL |
|------|--------|
| 某应用 JVM 堆内存使用（MB） | `qingzhou_heap_used{app="qingzhou-app-demo",model="jvm"}` |
| 跨实例堆内存最大值 | `max by (instance) (qingzhou_heap_used{model="jvm"})` |
| 某实例 CPU 使用率（%） | `qingzhou_cpu_process_usage{instance="<instanceId>"}` |
| 活跃线程数 | `qingzhou_thread{model="jvm"}` |

> 注意：非数值字段（如 `statsTime`、`uptime`）已被静默剔除，不会出现在指标流中；`field` 标签保留原始字段编码，可用于二次过滤。

## 五、排障 FAQ

**Q1：`curl /monitor/prometheus` 返回 404？**
确认 `qingzhou-monitor` bundle 已部署且启动日志包含 `http handler registered ... path: /prometheus`。路径前缀由 bundle SymbolicName 决定，必须是 `qingzhou-monitor`。

**Q2：Prometheus 抓取报 parser error？**
用 `promtool check metrics` 校验输出流定位行号：

```bash
curl -s http://localhost:7900/monitor/prometheus | promtool check metrics
```

常见原因为指标名非法字符或 HELP 未转义换行，本实现已在格式化引擎中统一清洗。

**Q3：远程实例指标缺失？**
远程采集依赖 agent 节点暴露 `/agent/monitor`，需满足：
1. agent 节点未禁用 `qingzhou-agent`（`-Dqingzhou.features.disabled` 不含该项）；
2. agent 已注册到管控台（管控台配置 `qingzhou-registry.private_key`，agent 配置 `qingzhou-agent.url` 与 `public_key`，密钥对用 `bin/gen-pair-key.sh` 生成）。

**Q4：远程实例拖慢整体抓取？**
单实例调用已设 3s 连接/读超时，超时或宕机的节点会被跳过，其余健康实例指标与 200 状态码不受影响。

**Q5：指标值出现 -1？**
部分监视字段在不可用时由应用返回 `-1`（如某些 OS 指标），属业务层语义，非采集错误。
