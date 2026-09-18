# Qingzhou Prometheus 监控适配自测报告

## 一、测试概述

| 项目 | 内容 |
|------|------|
| 被测模块 | `modules/qingzhou-monitor`（端点）、`modules/qingzhou-agent`（Monitor 组件） |
| 测试目标 | 验证监视数据以 Prometheus 标准格式统一暴露，可被 Prometheus 抓取接入 |
| 环境 | macOS（darwin）、OpenJDK 26、Maven 3.9.16、promtool 3.14.0 |
| 拓扑 | 单节点（管控台，agent 禁用）+ 双节点（管控台 7900 + 远程 agent 7901） |
| 校验工具 | `promtool check metrics`（官方语法/格式校验） |

## 二、六维度测试结果

| # | 维度 | 结果 | 验证要点 |
|---|------|------|---------|
| 1 | 端点可用性与安全放行 | ✅ | `GET /monitor/prometheus` 未登录返回 200；`Content-Type: text/plain; version=0.0.4; charset=utf-8`；日志 `path: /prometheus (no auth)` |
| 2 | 拓扑发现与批量采集 | ✅ | 单机发现 4 个应用（demo/nacos/zookeeper/redis）；双节点下远程实例经 `/agent/monitor` 一次拉取 |
| 3 | 协议规范与语法校验 | ✅ | `promtool check metrics` 退出码 0、零警告；HELP 取自 AppMeta 的 `name`，换行/反斜杠转义合规 |
| 4 | 多维标签与时序唯一性 | ✅ | 每行含 `instance/app/model/field` 四标签；双节点同名 App 形成相互隔离的两组时序 |
| 5 | 数据清洗与标量过滤 | ✅ | 数值无损；布尔映射 1.0/0.0；非数值字段静默剔除 |
| 6 | 容灾与韧性降级 | ✅ | 远程 agent 宕机后主流程捕获异常跳过，端点仍返回 200，健康实例指标不受影响 |

## 三、协议校验（promtool）

### 3.1 单节点 `/monitor/prometheus`

```bash
curl -s http://localhost:7900/monitor/prometheus | promtool check metrics
# 退出码 0，无任何语法/格式警告
```

- 数据行：59 条
- 唯一指标名：47 个（HELP/TYPE 各 47 条，无重复）
- 指标名规范化：camelCase → snake_case（`heapUsed` → `qingzhou_heap_used`），并剥离保留后缀（`threadCount` → `qingzhou_thread`、`slowLogTotal` → `qingzhou_slow_log`）

### 3.2 双节点聚合输出

```bash
curl -s http://localhost:7900/monitor/prometheus | promtool check metrics
# 退出码 0，无重复 HELP/TYPE
```

聚合后实例标签分布：

| instance | 数据行数 | 来源 |
|----------|---------|------|
| `-` | 59 | 管控台本地应用 |
| `F89F9F2AF82860AD64AAF4FA7513D315` | 34 | 远程 agent（Heartbeat 生成的实例 ID） |

同名 App（`qingzhou-app-demo`）在两个实例上形成独立的时序，`instance` 标签隔离有效。

## 四、容灾降级验证

| 场景 | 操作 | 结果 |
|------|------|------|
| 远程 agent 进程宕机 | 停止 agent 节点 | 管控台 `/monitor/prometheus` 仍返回 200，仅本地 59 条指标，远程节点被跳过，耗时 <0.1s |
| 单实例调用超时 | 代码 + 单元测试 | 远程调用设置 `connectTimeout(3000)` / `readTimeout(3000)`，超时被 try-catch 捕获 |
| 单 App/Model 异常 | 代码 | 每个 model 的 `invokeApp` 独立 try-catch，单点异常不中断整体 |

## 五、单元测试

`modules/qingzhou-monitor/src/test/java/qingzhou/monitor/PrometheusEndpointTest.java`，共 10 个用例，全部通过：

| 用例 | 覆盖场景 |
|------|---------|
| `numericValue_handle_losslessOutput` | 数值无损输出 |
| `booleanValue_handle_mapsToOneZero` | 布尔 → 1.0/0.0 |
| `nonNumericValue_handle_skipped` | 非数值/Null 剔除 |
| `camelCaseField_handle_snakeCaseMetricName` | camelCase → snake_case |
| `reservedSuffixField_handle_strippedMetricName` | `_count`/`_total` 保留后缀剥离 |
| `totalPrefixField_handle_preservedMetricName` | `total_` 前缀保留 |
| `helpWithNewline_handle_escaped` | HELP 换行转义 |
| `sharedFieldAcrossModels_handle_singleHelpType` | 同字段跨 model 单 HELP/TYPE |
| `remoteInstance_handle_aggregatedWithTimeout` | 远程聚合 + 3s 超时注入 |
| `remoteInstance_handle_deduplicatedHelpType` | 跨实例 HELP/TYPE 去重 |

```bash
mvn test -pl modules/qingzhou-monitor -am -Dtest=PrometheusEndpointTest
# Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
```

## 六、结论

六维度全部达标。端点免认证可抓取，输出通过 `promtool check metrics` 零警告校验；单节点与双节点场景下的本地采集、远程一次性拉取、跨实例时序隔离、脏数据过滤与容灾降级均验证通过。
