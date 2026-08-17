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
| 0 | dashboard | 首页 / Dashboard | 详情（Show） |
| 1 | applications | 应用管理 / Applications | 列表（List） |
| 2 | connector | Connector 通道 / Connector | 列表、详情、新增、修改、删除 |
| 3 | threadpool | 线程池 / Thread Pools | 列表、详情 |
| 4 | logs | 日志查看 / Log Viewer | 列表、详情 |

### 首页（dashboard）

显示当前探测到的 Tomcat 安装路径，用于快速确认探测结果。

字段：

| 字段 | 说明 |
|------|------|
| 安装路径 | `getAppContext().getDetectedPath()` 返回的 Tomcat 根目录 |

### 应用管理（applications）

从 `conf/context.xml` 解析 `<Context>` 节点，并扫描 `webapps/` 目录，合并展示已部署应用。

字段：

| 字段 | 说明 |
|------|------|
| 应用名称 | 从 `path` 属性提取（去掉开头 `/`，空则显示 `ROOT`） |
| 上下文路径 | 应用访问路径，如 `/demo` |
| 文档基目录 | 应用 docBase |
| 可重载 | 是否自动重载（`true` / `false`） |
| 应用状态 | `deployed`（docBase 目录存在）或 `not_found`（不存在） |

> 此模型仅支持列表查看，不可新增或修改。`webapps/` 下后缀为 `.war`、`.xml`、`.txt` 的文件，以及 `work`、`temp`、隐藏文件等会被自动跳过。
> `appBase` 来自 `server.xml` 中 `<Engine><Host>` 的 `appBase` 属性，相对路径会自动拼接到 Tomcat 根目录下。

### Connector 通道（connector）

支持对 `conf/server.xml` 中 `<Service><Connector>` 节点进行完整的增删改查。

字段：

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | string | - | - | Connector ID（端口，只读） |
| protocol | select | 否 | `HTTP/1.1` | Connector 使用的协议 |
| port | port | 是 | - | 监听端口（不可修改，新增时确定） |
| address | host | 否 | `0.0.0.0` | 绑定地址 |
| maxThreads | numeric | 否 | `200` | 最大工作线程数 |
| minSpareThreads | numeric | 否 | `10` | 最小空闲线程数 |
| acceptCount | numeric | 否 | `100` | 等待队列长度 |
| connectionTimeout | numeric | 否 | `20000` | 连接超时时间（毫秒） |
| redirectPort | port | 否 | `8443` | SSL 重定向端口 |

操作说明：

- **新增**：通过端口校验避免重复添加，相同端口的 Connector 会被自动跳过；新增节点会写入 `<Service>` 节点下。
- **修改**：以端口为唯一标识（id）定位节点，更新其属性。
- **删除**：以端口为唯一标识删除对应 `<Connector>` 节点。
- **列表/详情**：解析 `server.xml` 中所有 `<Connector>` 节点并支持按字段搜索。

### 线程池（threadpool）

从 `conf/server.xml` 解析 `<Executor>` 节点，展示线程池配置。仅支持列表和详情查看。

字段：

| 字段 | 说明 |
|------|------|
| 池名称 | Executor 的 `name` 属性 |
| 池前缀 | Executor 的 `namePrefix` 属性 |
| 最大线程 | `maxThreads`（默认 `200`） |
| 最小空闲 | `minSpareThreads`（默认 `10`） |

> 池名称作为 id 在详情查看时会进行 URL 解码处理。

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

本应用所有读写操作均针对以下 Tomcat 自带文件，未引入额外的运行时数据存储：

| 文件 | 用途 | 涉及操作 |
|------|------|----------|
| `<TOMCAT_HOME>/conf/server.xml` | Connector / Executor / Host appBase 配置 | 解析、新增、修改、删除 |
| `<TOMCAT_HOME>/conf/context.xml` | 应用 `<Context>` 配置 | 解析 |
| `<TOMCAT_HOME>/logs/` | 日志文件目录 | 读取 |
| `<TOMCAT_HOME>/webapps/` | 已部署应用目录 | 扫描 |

## 常见问题

**Q: 首页"安装路径"为空，所有页面提示 "tomcat path is empty"？**

A: 未探测到 Tomcat 安装目录。请确认：
1. 设置了环境变量 `CATALINA_HOME` 或 `TOMCAT_HOME` 指向 Tomcat 根目录；或
2. Tomcat 安装目录位于 `pom.xml` 中 `Qingzhou-Detection-Scan-Roots` 指定的扫描根之下，且包含 `bin/catalina.sh`（或 `bin/catalina.bat`）和 `conf/server.xml` 特征文件。

**Q: 应用管理列表为空？**

A: 检查 `conf/context.xml` 是否存在 `<Context>` 节点，以及 `webapps/` 目录是否包含已部署应用。`appBase` 取自 `server.xml` 中 `<Engine><Host>` 节点的 `appBase` 属性，若该属性缺失则无法解析相对路径的应用。

**Q: Connector 新增不生效？**

A: 新增时会按端口校验，若 `server.xml` 中已存在相同 `port` 的 `<Connector>` 节点会被静默跳过。请改用「修改」操作或更换端口。同时确认 `<Service>` 节点存在，新节点会被挂到该节点下。

**Q: Connector 修改后端口被覆盖？**

A: 修改接口以原端口作为唯一标识（id），并将 `port` 属性强制写回为原端口值，因此无法通过「修改」直接变更端口。如需变更端口，请删除旧 Connector 后重新「新增」。

**Q: 日志列表为空？**

A: 应用仅扫描 `logs/` 目录下后缀为 `.log` 或 `.txt` 的常规文件。其他后缀文件、子目录、隐藏文件均不会被读取。详情页加载时会读取整个文件内容，超大日志文件加载可能较慢。