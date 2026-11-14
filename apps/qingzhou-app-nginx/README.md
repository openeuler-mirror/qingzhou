# Qingzhou App Nginx

本机 nginx 日常运维应用：状态查看、启停重载、配置校验与编辑、日志查看。基于轻舟 `ModelBase` + 注解实现，
不编写独立 Controller 或前端，REST 与管控台动作由平台统一生成。

## 功能

| 顺序 | 模型 code | 名称 | 能力 |
| --- | --- | --- | --- |
| 0 | `dashboard` | 概览 | 安装目录、可执行文件、主配置、PID 文件、版本、运行状态、主进程、worker 数、编译参数 |
| 1 | `service` | 服务控制 | 启动、停止（quit）、强制停止（stop）、重载、重启、`nginx -t` 配置校验 |
| 2 | `config` | 配置管理 | 列举主配置及 `conf.d`/`sites-enabled` 等 `*.conf`，查看与在线编辑；保存前对完整主配置执行 `nginx -t`，失败回滚 |
| 3 | `logs` | 日志查看 | 列举 access/error 等日志，查看尾部内容（单次最多 2 MB） |

设计约束：

- 只做常规运维，不提供 nginx 安装、TLS 证书签发、upstream/站点可视化编辑等重功能。
- 不拼接 shell，所有命令通过 `ProcessBuilder` 参数数组执行，配置内容只交给 nginx 自身解析。
- 配置和日志只允许访问安装目录、主配置目录（日志还包括日志目录）内的真实文件，拒绝 `..` 与越界符号链接。

## nginx 路径解析

### 默认：轻舟自动发现

默认构建在 `META-INF/MANIFEST.MF` 中启用轻舟自动发现，与 Tomcat 等内置应用一致。引擎每 5 秒探测一次，
命中的每个安装目录各启动一个应用实例（应用 code 追加 `[序号]`），应用通过 `AppContext.getDetectedPath()`
管理各自实例。

| 清单属性 | 本应用取值 |
| --- | --- |
| `Qingzhou-Detection-Enabled` | `true`（默认；`-Pno-detection` 构建为 `false`） |
| `Qingzhou-Detection-Feature-Files` | `conf/nginx.conf`、`bin/nginx`、`bin/nginx.exe`、`sbin/nginx`、`sbin/nginx.exe`、`nginx.exe` |
| `Qingzhou-Detection-Env-Vars` | `NGINX_HOME`、`NGINX_PREFIX`（变量值须为安装目录） |
| `Qingzhou-Detection-Scan-Roots` | `user.home`、`/usr`、`/usr/local`、`/opt`、`/opt/homebrew/opt`、`/opt/homebrew`、`/usr/share`、`/etc`，以及 Windows 的 `C:\nginx`、`C:\Program Files\nginx`、`C:\Program Files (x86)\nginx`、`C:\ProgramData\nginx` |
| `Qingzhou-Detection-Process-Names` | `sbin/nginx`、`bin/nginx`、`nginx.exe` |
| `Qingzhou-Detection-Process-Home-Regex` | `-p\s+(\S+)`（优先从 `nginx -p <prefix>` 提取安装目录） |

说明：

- 扫描根会扫描根目录自身及其一级子目录；`user.home` 会替换为当前用户主目录。
- 清单中的扫描根与 JVM 参数 `-DQingzhou-Detection-Scan-Roots` **合并**，全局参数不会覆盖应用自身的常用路径。
- 进程关键字选择 `sbin/nginx`/`bin/nginx` 而不是 `nginx: master process`，因为 `ps` 输出可能以
  `nginx: master process` 开头，而引擎按“关键字之前的片段”回退定位目录；带路径的二进制片段更可靠。
- 命中后 `getDetectedPath()` 是该 bundle 实例的唯一身份，应用优先使用它，不再读取共享的 `app~...path`，
  避免多实例全部操作同一个目录。

### 显式配置模式

如果 nginx 安装在扫描根之外、未运行且未设置 `NGINX_HOME`，默认自动发现不会启动应用。此时使用
`no-detection` profile 构建，或把安装目录加入 `Qingzhou-Detection-Scan-Roots`：

```bash
# 构建不启用引擎探测的版本，应用随轻舟常规启动
mvn -pl apps/qingzhou-app-nginx -am -Pno-detection clean package -DskipTests
```

在 `instances/<实例>/conf/qingzhou.properties` 中配置：

```properties
# 安装目录（推荐）或 nginx 可执行文件；也支持 conf/nginx.conf 路径（需能在 PATH 找到二进制）
app~qingzhou-app-nginx.path=/usr/local/nginx
```

`no-detection` 模式下的解析顺序：

1. 应用配置：`path`（主键），兼容 `nginxPath`、`home`、`prefix`；
2. 环境变量 `NGINX_HOME`、`NGINX_PREFIX`；
3. 运行中的 nginx master 进程（Linux/macOS 读 `ps`，Windows 读 PowerShell CIM，并尽量解析 `-p`/`-c`）；
4. `PATH` 中的 `nginx` / `nginx.exe`；
5. 常见安装目录：`/usr`、`/usr/local`、`/usr/local/nginx`、`/opt/nginx`、`/opt/homebrew`、
   `/opt/homebrew/opt/nginx`、`/usr/local/opt/nginx`、`/usr/local/openresty/nginx`、`/usr/share/nginx`、
   `/etc/nginx`、`~/nginx`，Windows 的 `C:\nginx`、`%ProgramFiles%\nginx` 等。

显式配置、环境变量一旦给出，解析失败会直接报错，**不会**再静默回落到进程/PATH/常见目录，避免把启停动作
作用到另一套 nginx；检测到多个运行中的 nginx master 且未显式配置时也会报错要求指定实例。

## 构建与部署

```bash
# 默认：带自动发现（推荐）
mvn -pl apps/qingzhou-app-nginx -am clean package -DskipTests

# 或：显式配置路径，不启用引擎探测
mvn -pl apps/qingzhou-app-nginx -am -Pno-detection clean package -DskipTests

# 产物
apps/qingzhou-app-nginx/target/qingzhou-app-nginx.jar

# 部署：放入实例 apps 目录后重启轻舟
cp apps/qingzhou-app-nginx/target/qingzhou-app-nginx.jar \
   <产品包>/instances/<实例>/apps/
```

切换 `no-detection` profile 时请使用 `clean`，避免 Maven 复用上一种清单。`qingzhou-api` 以 `provided`
方式依赖，由平台运行时提供。应用图标为 `src/main/resources/icons/nginx.svg`。

## 使用前提与权限

- 执行轻舟实例的系统用户须能执行 nginx 二进制、读取配置与日志、向 master 进程发信号。
- 启动 80/443 等特权端口或使用系统 nginx 时，通常需要 root 或相应的 capabilities/权限配置。
- 配置编辑要求该用户对目标文件及其所在目录有写权限（需要创建临时文件并 rename）；编辑会先备份并临时替换目标文件，
  再对**完整主配置**执行 `nginx -t`（因此 `conf.d` 等被 include 的片段也能正确校验），失败自动回滚；上一次内容保留在
  `<实例>/temp/apps/nginx/backups/`。替换前会写 pending journal，若进程在替换与回滚之间退出，应用下次启动会用备份恢复。
- PID 文件来自主配置 `pid` 指令或编译期 `--pid-path`，须与实际启动方式一致，否则状态会显示 `stopped`。
  本应用不解析所有 include 中的 `pid` 指令。

## 兼容性

命令与解析只依赖 nginx 稳定接口（`-p`、`-c`、`-t`、`-s`、`-v`、`-V`、pid 文件），不解析版本相关的业务输出，
因此适用于 nginx 1.26.x / 1.30.x 及相近版本。已用仓库内的 `nginx-1.26.3.tar.gz`、`nginx-1.30.6.tar.gz`
编译安装后验证：

- 启动、优雅停止、强制停止、重启、重载；
- `nginx -t` 校验、配置读取与保存、非法配置拒绝且文件不被覆盖；
- worker 数、主进程 PID、版本、日志读取；
- 配置/日志路径穿越拒绝；
- 默认构建通过 `NGINX_HOME` 与运行中进程两种来源识别实例，`no-detection` 构建通过配置路径管理实例。

验证环境因缺少 pcre/zlib 开发包，nginx 以
`--without-http_rewrite_module --without-http_gzip_module` 编译；应用本身不依赖这两个模块。

跨平台说明：

- Linux/macOS：使用 `ps`、`kill -0`、`nginx -s`。
- Windows：使用 `tasklist` 判断进程、PowerShell CIM 查询子进程、`nginx.exe` 与 `nginx -s`；路径统一经 `java.nio.file.Path` 处理。
- 配置文本按 UTF-8 读写；命令输出按 JVM 默认编码读取。

## 安全说明

- 不接受页面传入的命令、二进制路径或任意文件路径；可访问文件由安装目录枚举产生。
- 配置保存前对完整主配置执行 `nginx -t`，失败自动回滚并返回错误；重载只保证信号已发送，是否生效以 error.log 为准。
- 未被主配置 `include` 的 `*.conf` 也会列出，但 `nginx -t` 不会解析它；请确认 include 关系后再编辑这类文件。
- 状态判断和信号发送会校验 PID 对应的是 nginx master，避免 pid 文件过期/PID 复用时误发信号。
- 配置在线编辑上限约 1 MB（对齐框架单参数上限），日志单次查看上限 2 MB；超出时配置只读、日志只取尾部，避免大文件占满堆内存。
- `nginx -t` 会加载配置中的模块指令，等同于让 nginx 解析该配置；这是配置管理功能的固有边界。
- 应用级角色为 `admin`，启停、编辑等动作默认继承该角色，不额外放宽权限。

## 目录中的其他文件

`nginx-1.26.3.tar.gz`、`nginx-1.30.6.tar.gz` 是验证用 nginx 源码包，不打入应用 jar；`nginx.svg` 为应用原始图标，
构建时从 `src/main/resources/icons/nginx.svg` 打入。
