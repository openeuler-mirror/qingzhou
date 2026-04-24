# Nginx 应用功能说明

## 概述

qingzhou-nginx-app 是一个用于管理本地 Nginx 服务器的轻舟应用，提供配置查看、修改、备份和实时监控功能。

## 功能特性

- ✅ **实时监控**: JVM 运行状态和 Nginx 连接状态监控
- ✅ **配置管理**: 查看和修改 20+ 常用 Nginx 配置项
- ✅ **配置备份**: 自动备份和历史恢复功能
- ✅ **智能路径管理**: 支持开发模式（文件系统）和生产模式（JAR包）
- ✅ **配置中心化管理**: 统一的 AppConfig 配置管理类

## 功能模块

### 1. Home（首页）- 应用简要运行信息

**模型代码**: `home`  
**排序**: 1  
**菜单位置**: 基础功能 → 首页

**功能**: 显示应用实时运行状态

**监控指标**:
- 统计时间 (Stats Time)
- 应用运行时长 (Duration) - 自动格式化为 `Xd Xh Xm Xs`

**实现**: 通过 Java Management API 获取当前应用运行时信息

---

### 2. NginxStatus（Nginx 连接状态）- 实时监控 🔗

**模型代码**: `nginxStatus`  
**排序**: 2  
**菜单位置**: 基础功能 → Nginx 连接状态  
**动作类型**: monitor

**功能**: 实时显示 Nginx 连接状态（通过 stub_status 模块）

**监控指标**:
| 指标 | 说明 |
|------|------|
| 活动连接数 | 当前所有处于打开状态的活动连接数 |
| 接收连接数 | Nginx 启动以来已经接收的连接总数 |
| 处理连接数 | Nginx 启动以来已经处理的连接总数 |
| 处理请求数 | Nginx 启动以来已经处理的请求总数 |
| 接收请求中连接 | 正处于接收请求阶段的连接数 |
| 响应请求中连接 | 请求已接收完成，处于响应过程的连接数 |
| 活动状态的连接 | 保持连接模式下，处于活动状态的连接数 |

**数据来源**: HTTP 请求获取 Nginx stub_status 页面

**配置**: `AppConfig.getNginxStatusUrl()` 默认 `http://localhost/nginx_status`

**前提条件**: 
- Nginx 需配置 stub_status 模块（注意：nginx 1.30.0 版本已经移除了此模块）
- 示例配置:
```nginx
location /nginx_status {
    stub_status;
    allow 127.0.0.1;
    deny all;
}
```

---

### 3. NginxSettings（配置管理）- 查看和修改配置 ⚙️

**模型代码**: `settings`  
**排序**: 3  
**菜单位置**: 基础功能 → 配置管理  
**实现接口**: List, Show, Update

**功能**: 
- ✅ 查看 Nginx 常用配置项
- ✅ 修改配置项值
- ✅ 搜索配置项
- ✅ 自动备份原配置

**管理的配置项 (20+)**:

#### 全局配置 (main) - 4项
| 配置项 | 说明 | 示例值 |
|--------|------|--------|
| user | Nginx进程运行用户 | nginx |
| worker_processes | 工作进程数（auto=CPU核数） | auto |
| error_log | 错误日志路径和级别 | /var/log/nginx/error.log warn |
| pid | 进程PID保存路径 | /run/nginx.pid |

#### Events区块配置 - 1项
| 配置项 | 说明 | 示例值 |
|--------|------|--------|
| worker_connections | 每个工作进程最大并发连接数 | 1024 |

#### Stream区块配置 - 1项
| 配置项 | 说明 | 示例值 |
|--------|------|--------|
| stream.listen | Stream监听端口 | 443 |

#### HTTP区块配置 - 14项
| 配置项 | 说明 | 示例值 |
|--------|------|--------|
| access_log | 访问日志路径和格式 | /var/log/nginx/access.log main |
| gzip | 是否启用压缩（on/off） | on |
| gzip_min_length | 最小压缩文件大小 | 1k |
| gzip_types | 压缩的MIME类型 | text/plain application/json |
| keepalive_timeout | 保持连接超时时间 | 65 |
| client_max_body_size | 客户端最大请求体大小 | 10m |
| log_format | 日志格式定义 | main '$remote_addr...' |
| default_type | 默认MIME类型 | application/octet-stream |
| proxy_connect_timeout | 代理连接超时时间 | 6s |
| proxy_send_timeout | 代理发送超时时间 | 10s |
| proxy_read_timeout | 代理接收超时时间 | 10s |
| proxy_buffer_size | 代理缓冲区大小 | 16k |
| proxy_buffers | 代理缓冲区个数和大小 | 4 32k |
| proxy_busy_buffers_size | 忙碌缓冲区大小 | 64k |
| proxy_temp_file_write_size | 代理临时文件写入大小 | 16k |

**使用示例**:

1. **查看配置列表**
   - 访问配置管理页面
   - 显示所有可管理的配置项

2. **搜索配置**
   - 在搜索框输入关键词（如"proxy"）
   - 快速定位相关配置

3. **修改配置**
   - 点击配置项查看详情
   - 修改配置值
   - 保存后自动备份原配置文件

4. **配置生效**
   - 修改后需重载Nginx配置
   - 执行: `nginx -s reload`

---

### 4. NginxBackup（配置备份）- 备份管理 💾

**模型代码**: `backup`  
**排序**: 4  
**菜单位置**: 基础功能 → 配置备份  
**实现接口**: List, Show

**功能**:
- ✅ 查看配置备份历史
- ✅ 查看备份文件内容
- ✅ 恢复历史备份
- ✅ 删除过期备份

**备份策略**:
- 每次修改配置前自动备份
- 备份文件命名: `nginx.conf.YYYYMMDD_HHMMSS`
- 备份目录: 临时目录下的 backups 文件夹（由 AppConfig.getNginxBackupPath() 动态获取）

**使用示例**:

1. **查看备份列表**
   - 访问配置备份页面
   - 按时间倒序显示所有备份

2. **查看备份内容**
   - 点击备份文件
   - 查看完整的配置文件内容

3. **恢复备份**
   ```java
   NginxBackup backup = new NginxBackup();
   backup.restoreBackup("nginx.conf.20260424_143022");
   ```

4. **手动创建备份**
   ```java
   NginxBackup backup = new NginxBackup();
   backup.createBackup();
   ```

---

## 架构设计

### 核心类

#### 1. AppNginx - 应用入口类 🚀

**职责**: 应用启动和停止管理

**启动流程**:
```java
@Override
public void start(AppContext appContext) {
    // 1. 创建临时目录和备份目录
    appContext.getTemp().mkdirs();
    new File(appContext.getTemp(), "backups").mkdirs();
    
    // 2. 将 nginx.conf 从 resources 拷贝到临时目录
    Path nginxConf = new File(appContext.getTemp(), "nginx.conf").toPath();
    URL nginxConfUrl = getClass().getClassLoader().getResource("nginx.conf");
    Files.copy(nginxConfUrl.openStream(), nginxConf, REPLACE_EXISTING);
    
    // 3. 配置路径到 AppConfig（统一管理）
    AppConfig.getConfig().setProperty(NGINX_CONF_PATH_KEY, nginxConf.toString());
    AppConfig.getConfig().setProperty(NGINX_CONF_BACKUPS_KEY, backupsPath);
}
```

**特点**:
- ✅ 自动处理 JAR 包和开发两种模式（通过 URL 协议自动识别）
- ✅ 使用轻舟平台临时目录，应用隔离
- ✅ 配置集中化管理到 AppConfig

#### 2. AppConfig - 配置管理中心 ⚙️

**职责**: 统一管理所有配置项和路径

**配置项**:
| 常量 | Key | 说明 | 默认值 |
|------|-----|------|--------|
| NGINX_STATUS_URL_KEY | nginx_status_url | Nginx状态检查URL | http://localhost/nginx_status |
| NGINX_PATH_KEY | nginx.path | Nginx可执行文件路径 | 空 |
| NGINX_CONF_PATH_KEY | nginx.path.conf | nginx.conf路径 | 启动时动态设置 |
| NGINX_CONF_BACKUPS_KEY | nginx.path.backups | 备份目录路径 | 启动时动态设置 |

**主要方法**:
```java
// 获取所有配置（Properties对象）
Properties getConfig()

// 获取指定配置项
String getConfig(String key)

// 获取Nginx状态URL
String getNginxStatusUrl()

// 获取Nginx配置文件路径（由AppNginx启动时设置）
String getNginxConfPath()

// 获取备份目录路径（由AppNginx启动时设置）
String getNginxBackupPath()
```

**特点**:
- ✅ 静态初始化时自动加载 config.properties
- ✅ 提供类型安全的 getter 方法
- ✅ 支持运行时动态设置配置（AppNginx启动时设置路径）
- ✅ 所有组件通过 AppConfig 统一访问配置

#### 3. NginxConfigParser - 配置解析工具 🔧

**职责**: 解析和修改 nginx.conf 配置文件

**主要方法**:
```java
// 解析配置文件，提取20+常用配置项
Map<String, String> parseConfig()

// 更新配置项（自动备份）
void updateConfig(String directive, String newValue)

// 备份配置
void backupConfig()

// 获取备份列表
List<String> getBackupList()

// 获取配置描述说明
Map<String, String> getConfigDescriptions()

// 获取配置项所属区块（main/events/http/stream）
String getConfigBlock(String directive)
```

**实现原理**:
- ✅ 配置定义集中化：CONFIG_DEFINITIONS 统一管理20+配置项
- ✅ 使用正则表达式解析 nginx.conf
- ✅ 支持全局指令和块内指令的提取和修改
- ✅ 修改前自动备份原文件
- ✅ 通过 AppConfig.getNginxConfPath() 动态获取配置文件路径

### 文件结构

```
qingzhou-nginx-app/
├── src/main/java/qingzhou/app/nginx/
│   ├── AppNginx.java              # 应用入口（启动/停止）
│   ├── AppConfig.java             # 配置管理中心
│   ├── Home.java                  # JVM监控首页
│   ├── NginxStatus.java           # Nginx连接状态监控
│   ├── NginxSettings.java         # 配置管理模型（查看/修改）
│   ├── NginxBackup.java           # 配置备份模型（查看/恢复）
│   └── NginxConfigParser.java     # 配置解析工具类
├── src/main/resources/
│   ├── config.properties          # 应用配置文件
│   └── nginx.conf                 # Nginx配置模板（部署到临时目录）
└── target/
    └── qingzhou-nginx-app-*.jar   # 打包后的JAR文件
```

### 运行时文件（临时目录）

```
{轻舟临时目录}/
├── nginx.conf                     # 实际使用的Nginx配置文件
└── backups/                       # 备份目录
    ├── nginx.conf.20260424_143022
    ├── nginx.conf.20260424_145011
    └── ...
```

---

## 启动流程详解

### 应用启动（AppNginx.start）

1. **创建目录结构**
   ```java
   appContext.getTemp().mkdirs();
   new File(appContext.getTemp(), "backups").mkdirs();
   ```

2. **拷贝 nginx.conf 到临时目录**
   - 从 resources/nginx.conf 读取
   - 拷贝到临时目录（支持 JAR 和文件系统两种模式）
   - 通过 URL 协议自动识别：file / jar

3. **配置路径到 AppConfig**
   ```java
   AppConfig.getConfig().setProperty(NGINX_CONF_PATH_KEY, nginxConfPath);
   AppConfig.getConfig().setProperty(NGINX_CONF_BACKUPS_KEY, backupsPath);
   ```

4. **打印启动信息**
   - 应用版本号、根路径、临时目录等

### 查看和修改配置
1. 登录轻舟管理控制台
2. 进入Nginx应用
3. 点击"配置管理"菜单
4. 浏览或搜索配置项

### 修改配置
1. 在配置管理列表中找到目标配置项
2. 点击查看详情
3. 修改配置值
4. 点击保存（自动备份原配置）
5. 重载Nginx: `nginx -s reload`

### 恢复配置备份
1. 进入"配置备份"菜单
2. 选择要恢复的备份文件
3. 查看备份内容确认
4. 执行恢复操作
5. 重载Nginx: `nginx -s reload`

---

## 注意事项

### ⚠️ 重要提醒

1. **配置修改后需重载**
   - 修改配置文件不会自动生效
   - 必须执行 `nginx -s reload` 或 `systemctl reload nginx`

2. **临时目录机制**
   - nginx.conf 在轻舟平台临时目录中（应用隔离）
   - 备份目录也在临时目录下（backups 子目录）
   - 应用卸载时临时目录可能被清理，注意备份重要配置

3. **配置验证**
   - 修改后建议先验证配置: `nginx -t`
   - 验证通过后再重载

4. **权限要求**
   - 读取 nginx.conf 需要读权限
   - 修改配置文件需要写权限（临时目录通常有写权限）
   - 备份目录需要创建和写入权限

5. **正则解析限制**
   - 当前使用正则表达式解析
   - 对于复杂嵌套配置可能不够精确
   - 建议只修改简单指令

### 安全建议

1. 修改前务必备份
2. 测试环境先验证
3. 记录所有修改操作
4. 保留足够的历史备份

---

## 测试

运行单元测试:
```bash
cd /home/qingzhou/workspace/tongtech/qingzhou/apps/qingzhou-nginx-app
mvn test
```

测试结果:
```
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
✅ BUILD SUCCESS
```

---

## 扩展建议

### 未来可增强的功能

1. **配置验证**
   - 修改后自动执行 `nginx -t` 验证
   - 验证失败则回滚

2. **重载Nginx**
   - 提供一键重载功能
   - 显示重载结果

3. **配置对比**
   - 对比当前配置与备份的差异
   - 高亮显示修改内容

4. **模板管理**
   - 提供常用配置模板
   - 快速应用最佳实践配置

5. **权限控制**
   - 限制某些敏感配置的修改
   - 操作审计日志

6. **实时监控**
   - WebSocket推送状态变化
   - 异常告警通知

---

## 常见问题

### Q1: 修改配置后不生效？
**A**: 需要重载Nginx配置：
```bash
nginx -s reload
# 或
systemctl reload nginx
```

### Q2: 配置修改后Nginx启动失败？
**A**: 
1. 检查配置语法: `nginx -t`
2. 从备份恢复: 使用配置备份功能
3. 查看错误日志: `/var/log/nginx/error.log`

### Q3: 看不到某些配置项？
**A**: 当前只解析了常用配置项，如需更多配置项，可在 `NginxConfigParser.java` 中的 `CONFIG_DEFINITIONS` 添加。

### Q4: 备份文件在哪里？
**A**: 轻舟平台临时目录下的 backups 文件夹，可通过 `AppConfig.getNginxBackupPath()` 获取路径。

### Q5: nginx.conf 在哪个目录？
**A**: 应用启动时从 resources 拷贝到轻舟平台临时目录，可通过 `AppConfig.getNginxConfPath()` 获取路径。

---

## 开发者信息

**创建日期**: 2026-04-24  
**版本**: 1.0.0  
**作者**: Qingzhou Team  

**依赖**:
- qingzhou-api
- Java 8+
- Nginx

**测试覆盖**: 7个单元测试用例
