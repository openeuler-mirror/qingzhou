# Qingzhou App Tomcat

Tomcat 日常运维管理控制台，提供连接器配置、应用部署、线程池监控、日志查看等功能。
本应用通过轻舟的自动探测机制定位本机 Tomcat 安装目录，无需在界面中手动新增实例。

## 安装

在项目根目录执行：

```
mvn clean install
```

构建产物 `qingzhou-app-tomcat-*.jar` 会输出到 `target/` 目录，将该 jar 放入轻舟产品包的 `apps/` 目录下即可随轻舟实例一起加载。

> 应用需要本机存在可访问的 Tomcat 安装目录，否则各功能页面会提示"tomcat path is empty"。

## Tomcat 实例探测

本应用不通过 `qingzhou.properties` 配置实例，而是依赖轻舟的自动探测机制定位 Tomcat 安装目录，探测规则在 `pom.xml` 的 `maven-jar-plugin` 清单中声明：

| 清单属性 | 说明 |
|----------|------|
| `Qingzhou-Detection-Enabled` | 是否启用探测，固定为 `true` |
| `Qingzhou-Detection-Feature-Files` | 用于识别 Tomcat 目录的特征文件：`bin/catalina.sh`、`bin/catalina.bat`、`conf/server.xml` |
| `Qingzhou-Detection-Env-Vars` | 用于识别 Tomcat 目录的环境变量：`CATALINA_HOME`、`TOMCAT_HOME` |
| `Qingzhou-Detection-Scan-Roots` | 探测扫描根目录，需要时按部署环境调整 |
| `Qingzhou-Detection-Process-Names` | 进程名匹配规则（当前为空） |

### 探测优先级

1. 优先读取环境变量 `CATALINA_HOME` 或 `TOMCAT_HOME` 指向的目录；
2. 若环境变量未设置，则在 `Qingzhou-Detection-Scan-Roots` 指定的根目录下递归扫描，命中特征文件（`bin/catalina.sh` + `conf/server.xml`）的目录即视为 Tomcat 安装目录；
3. 探测结果会显示在「首页」的"安装路径"字段中。

> 若首页未显示安装路径，请检查环境变量是否设置，或将 Tomcat 安装目录置于扫描根之下。

## 功能模块

应用按 `order` 顺序提供以下 5 个模型，对应控制台左侧菜单：

| 顺序 | 模型 code | 名称 | 支持操作 |
|------|-----------|------|----------|
| 0 | dashboard | 首页 / Dashboard | 详情、监控（Show、Monitor） |
| 1 | applications | 应用管理 / Applications | 列表（List） |
| 2 | connector | Connector 通道 / Connector | 列表、详情、监控 |
| 3 | threadpool | 线程池 / Thread Pools | 列表、详情 |
| 4 | logs | 日志查看 / Log Viewer | 列表、详情 |

### 首页（dashboard）

以监控卡片/图表展示 Tomcat 安装路径与运行中 MBean 的汇总指标。

字段：

| 字段 | 图表 | MBean 来源 |
|------|------|-----------|
| 统计时间 | 文本 | 采集时刻 |
| 安装路径 | 文本 | `getAppContext().getDetectedPath()` |
| 服务器版本 | 文本 | `Catalina:type=Server` 的 `serverInfo` |
| 通道数 | stat | `Catalina:type=Connector` 数量 |
| 应用数 | stat | `Catalina:j2eeType=WebModule` 数量 |
| 活跃会话数 | stat | `Catalina:type=Manager` 的 `activeSessions` 求和 |
| 累计请求数 | stat | `Catalina:type=GlobalRequestProcessor` 的 `requestCount` 求和 |
| 累计错误数 | stat | 同上 `errorCount` 求和 |
| 已发送 / 已接收(MB) | stat | 同上 `bytesSent` / `bytesReceived` 求和并换算 |
| 线程繁忙率(%) | gauge | ThreadPool 的 `currentThreadsBusy / maxThreads` |
| 繁忙线程 / 空闲线程 | bar | ThreadPool 的 `currentThreadsBusy` 与 `currentThreadCount - currentThreadsBusy` |
| 平均 / 最大处理时间(ms) | line | GlobalRequestProcessor 的 `processingTime / requestCount` 与 `maxTime` |

> 指标为累计值，反映 Tomcat 自启动以来的数据；线程与会话为实时值。

### 应用管理（applications）

通过本地 Attach 读取运行中 Tomcat 的 WebModule MBean；Tomcat 未运行或无法 attach 时页面直接报错。

字段：

| 字段 | 说明 |
|------|------|
| 应用名称 | 从 `path` 属性提取（去掉开头 `/`，空则显示 `ROOT`） |
| 上下文路径 | 应用访问路径，如 `/demo` |
| 文档基目录 | 应用 docBase |
| 可重载 | 是否自动重载（`true` / `false`） |
| 应用状态 | Tomcat 运行状态（`STARTED` 等） |

> 此模型仅支持列表查看，不可新增或修改。

列表操作列的「监视」按应用名读取该应用的运行时指标：

| 指标 | 图表 | MBean 来源 |
|------|------|-----------|
| 统计时间 | 文本 | 采集时刻 |
| 累计请求数 / 累计错误数 | stat | `WebModule` 的 `requestCount` / `errorCount` |
| 平均 / 最大 / 最小处理时间 | line / stat | `processingTime / requestCount`、`maxTime`、`minTime` |
| 活跃会话数 | stat | 同 `path` 的 `Manager` 的 `activeSessions` |
| 运行状态 | 文本 | `WebModule` 的 `stateName` |

### Connector 通道（connector）

只读运行中 Tomcat 的 Connector MBean，不读写任何配置文件。

字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string | Connector ID（端口，只读） |
| protocol | select | Connector 使用的协议 |
| port | port | 监听端口 |
| address | host | 绑定地址 |
| maxThreads | numeric | 最大工作线程数 |
| minSpareThreads | numeric | 最小空闲线程数 |
| acceptCount | numeric | 等待队列长度 |
| connectionTimeout | numeric | 连接超时时间（毫秒） |
| redirectPort | port | SSL 重定向端口 |

> 列表/详情读取 Connector MBean，支持按字段搜索。

列表操作列的「监控」按端口读取该通道的运行时指标：

| 指标 | 图表 | MBean 来源 |
|------|------|-----------|
| 统计时间 | 文本 | 采集时刻 |
| 累计请求数 / 累计错误数 | stat | 同名 `GlobalRequestProcessor` 的 `requestCount` / `errorCount` |
| 已发送 / 已接收 (MB) | stat | 同上 `bytesSent` / `bytesReceived` |
| 线程繁忙率 | gauge | 同端口 `ThreadPool` 的 `currentThreadsBusy / maxThreads` |
| 繁忙 / 空闲线程 | bar | `currentThreadsBusy`、`currentThreadCount - currentThreadsBusy` |
| 平均 / 最大处理时间 | line | `processingTime / requestCount`、`maxTime` |

### 线程池（threadpool）

只读运行中 Tomcat 的 ThreadPool MBean，字段与 MBean 实际暴露的属性一一对应。

字段：

| 字段 | MBean 属性 | 说明 |
|------|-----------|------|
| 池名称 | `name`（ObjectName key） | 线程池名称，作为 id |
| 端口 | `port` | 所属 Connector 端口 |
| 本地端口 | `localPort` | 实际监听端口 |
| 最大线程 | `maxThreads` | 配置的最大线程数 |
| 最小空闲 | `minSpareThreads` | 最小空闲线程数 |
| 当前线程数 | `currentThreadCount` | 当前已创建的线程数 |
| 繁忙线程数 | `currentThreadsBusy` | 正在处理请求的线程数 |
| 连接数 | `connectionCount` | 当前打开的连接数 |
| 长连接数 | `keepAliveCount` | 当前保持的长连接数 |
| 最大连接数 | `maxConnections` | 允许的最大连接数（详情） |
| 等待队列 | `backlog` | 等待接受的连接队列长度（详情） |
| 接受线程数 | `acceptorThreadCount` | Acceptor 线程数（详情） |
| 轮询线程数 | `pollerThreadCount` | Poller 线程数（详情） |
| 长连接超时 | `keepAliveTimeout` | 长连接超时时间（详情） |
| 最大长连接请求 | `maxKeepAliveRequests` | 单个长连接允许的最大请求数（详情） |
| 读取超时 | `soTimeout` | Socket 读取超时时间（详情） |
| 运行状态 | `running` | 线程池是否运行中（详情） |
| 暂停状态 | `paused` | 线程池是否已暂停（详情） |

> 池名称作为 id 在详情查看时会进行 URL 解码处理。

列表操作列的「监视」按池名称读取该线程池的运行时指标：

| 指标 | 图表 | MBean 属性 |
|------|------|-----------|
| 统计时间 | 文本 | 采集时刻 |
| 已创建线程 | stat | `currentThreadCount` |
| 繁忙 / 空闲线程 | bar | `currentThreadsBusy`、`currentThreadCount - currentThreadsBusy` |
| 线程繁忙率 | gauge | `currentThreadsBusy / maxThreads` |
| 当前连接数 / 长连接数 | stat | `connectionCount` / `keepAliveCount` |

### 日志查看（logs）

扫描 Tomcat `logs/` 目录下后缀为 `.log` 或 `.txt` 的文件，支持列表与详情查看（含全文内容）。

字段：

| 字段 | 说明 |
|------|------|
| 文件名 | 日志文件名 |
| 修改时间 | 最后修改时间（格式 `yyyy-MM-dd HH:mm:ss`） |
| 大小 | 自动换算为 B / KB / MB / GB |
| 类型 | 根据文件名识别：`SYSTEM`（catalina）、`APP`（localhost，非 access）、`ACCESS`、`MANAGER`、`HOST`、`OTHER` |
| 内容 | 详情页展示文件全部内容 |
| 行数 | 详情页展示日志总行数 |

## 依赖的 Tomcat 关键文件

运行期数据全部来自运行中 Tomcat 的 MBean，本应用为只读，不读取也不修改 Tomcat 配置文件：

| 文件 | 用途 | 涉及操作 |
|------|------|----------|
| `<TOMCAT_HOME>/logs/` | 日志文件目录 | 读取 |

## 常见问题

**Q: 首页"安装路径"为空，所有页面提示 "tomcat path is empty"？**

A: 未探测到 Tomcat 安装目录。请确认：
1. 设置了环境变量 `CATALINA_HOME` 或 `TOMCAT_HOME` 指向 Tomcat 根目录；或
2. Tomcat 安装目录位于 `pom.xml` 中 `Qingzhou-Detection-Scan-Roots` 指定的扫描根之下，且包含 `bin/catalina.sh`（或 `bin/catalina.bat`）和 `conf/server.xml` 特征文件。

**Q: 应用管理列表为空？**

A: 走 JMX，需确认 Tomcat 进程正在运行，Qingzhou 与 Tomcat 属同一操作系统用户（Attach 要求），且未配置 `-XX:+DisableAttachMechanism`。

**Q: Connector 列表与实际不符？**

A: 列表读取运行中 Tomcat 的 MBean，反映的是 Tomcat 启动时加载的配置。手工修改 `conf/server.xml` 后需重启 Tomcat 才会同步。

**Q: 日志列表为空？**

A: 应用仅扫描 `logs/` 目录下后缀为 `.log` 或 `.txt` 的常规文件。其他后缀文件、子目录、隐藏文件均不会被读取。详情页加载时会读取整个文件内容，超大日志文件加载可能较慢。