# AppNginx 启动流程说明

## 功能概述

`AppNginx.start()` 方法在应用启动时自动执行，完成以下核心任务：

1. **准备目录结构**: 创建临时目录和备份目录
2. **部署 nginx.conf**: 将 resources 中的 nginx.conf 拷贝到临时目录
3. **配置路径管理**: 将路径信息注册到 AppConfig 统一管理

## 启动流程详解

### 完整启动代码

```java
@Override
public void start(AppContext appContext) {
    System.out.println("\n========================================== nginx ====================================================");
    try {
        // 1. 创建临时目录
        appContext.getTemp().mkdirs();
        
        // 2. 打印应用配置
        appContext.getProperties().forEach((k, v) -> 
            System.out.println("nginx 应用配置 " + k + "=" + v));

        // 3. 创建备份目录
        new File(appContext.getTemp(), "backups").mkdirs();
        
        // 4. 部署 nginx.conf 到临时目录
        Path nginxConf = new File(appContext.getTemp(), "nginx.conf").toPath();
        nginxConf.toFile().createNewFile();
        URL nginxConfUrl = getClass().getClassLoader().getResource("nginx.conf");

        try (InputStream in = nginxConfUrl.openStream()) {
            Files.copy(in, nginxConf, StandardCopyOption.REPLACE_EXISTING);
        }

        // 5. 注册路径到 AppConfig（全局可访问）
        AppConfig.getConfig().setProperty(
            AppConfig.NGINX_CONF_PATH_KEY, 
            nginxConf.toString()
        );
        AppConfig.getConfig().setProperty(
            AppConfig.NGINX_CONF_BACKUPS_KEY, 
            new File(appContext.getTemp(), "backups").getAbsolutePath()
        );

        // 6. 打印启动信息
        System.out.println("nginx 应用版本号：" + appContext.getVersion());
        System.out.println("nginx 应用根路径：" + appContext.getBase());
        System.out.println("nginx 应用临时目录：" + appContext.getTemp().getAbsolutePath());
    } catch (IOException | UnsupportedOperationException e) {
        System.err.println("nginx 应用启动异常: " + e.getMessage());
    }
    System.out.println("========================================== nginx ====================================================\n");
}
```

### 步骤详解

#### 步骤 1: 创建临时目录

```java
appContext.getTemp().mkdirs();
```

**说明**:
- `appContext.getTemp()` 由轻舟平台提供
- 默认位置: `{qingzhou_home}/instances/{instance_name}/temp/nginx-app/`
- 每个应用有独立的临时目录，实现应用隔离
- `mkdirs()` 确保目录存在（包括父目录）

#### 步骤 2: 打印应用配置

```java
appContext.getProperties().forEach((k, v) -> 
    System.out.println("nginx 应用配置 " + k + "=" + v));
```

**说明**:
- 打印从平台传入的配置参数
- 便于调试和验证配置是否正确加载

#### 步骤 3: 创建备份目录

```java
new File(appContext.getTemp(), "backups").mkdirs();
```

**说明**:
- 在临时目录下创建 backups 子目录
- 用于存储配置修改前的自动备份
- 备份文件命名: `nginx.conf.YYYYMMDD_HHMMSS`

#### 步骤 4: 部署 nginx.conf

```java
Path nginxConf = new File(appContext.getTemp(), "nginx.conf").toPath();
nginxConf.toFile().createNewFile();
URL nginxConfUrl = getClass().getClassLoader().getResource("nginx.conf");

try (InputStream in = nginxConfUrl.openStream()) {
    Files.copy(in, nginxConf, StandardCopyOption.REPLACE_EXISTING);
}
```

**关键特性**:

##### 自动适配两种模式

| 模式 | 场景 | URL协议 | 处理方式 |
|------|------|---------|---------|
| **开发模式** | 未打包，直接运行 | `file` | 从文件系统读取 |
| **生产模式** | 打包成JAR | `jar` | 从JAR中解压 |

**自动识别机制**:
```java
URL nginxConfUrl = getClass().getClassLoader().getResource("nginx.conf");
// 开发模式: file:/home/qingzhou/.../src/main/resources/nginx.conf
// 生产模式: jar:file:/path/to/qingzhou-nginx-app.jar!/nginx.conf
```

##### 统一处理方式

无论哪种模式，都使用相同的拷贝代码：
```java
try (InputStream in = nginxConfUrl.openStream()) {
    Files.copy(in, nginxConf, StandardCopyOption.REPLACE_EXISTING);
}
```

**优势**:
- ✅ 代码简洁，不需要区分模式
- ✅ `openStream()` 自动处理不同协议
- ✅ `StandardCopyOption.REPLACE_EXISTING` 覆盖已存在的文件

#### 步骤 5: 注册路径到 AppConfig

```java
AppConfig.getConfig().setProperty(
    AppConfig.NGINX_CONF_PATH_KEY,     // "nginx.path.conf"
    nginxConf.toString()
);
AppConfig.getConfig().setProperty(
    AppConfig.NGINX_CONF_BACKUPS_KEY,  // "nginx.path.backups"
    new File(appContext.getTemp(), "backups").getAbsolutePath()
);
```

**说明**:
- 将运行时路径注册到 AppConfig
- 所有其他组件通过 AppConfig 获取路径
- 实现配置集中化管理

**注册的配置项**:
| Key | 值 | 用途 |
|-----|---|------|
| `nginx.path.conf` | 临时目录/nginx.conf | Nginx配置文件路径 |
| `nginx.path.backups` | 临时目录/backups | 备份目录路径 |

#### 步骤 6: 打印启动信息

```java
System.out.println("nginx 应用版本号：" + appContext.getVersion());
System.out.println("nginx 应用根路径：" + appContext.getBase());
System.out.println("nginx 应用临时目录：" + appContext.getTemp().getAbsolutePath());
```

**说明**:
- 便于调试和验证启动状态
- 提供关键路径信息

## AppConfig 配置管理

### 配置初始化

```java
public class AppConfig {
    private static final Properties APP_CONFIG = new Properties();
    
    static {
        // 读取 config.properties 配置
        URL configUrl = AppConfig.class.getClassLoader()
            .getResource("config.properties");
        if (configUrl != null) {
            try (InputStream in = configUrl.openStream()) {
                Properties defaultProps = new Properties();
                defaultProps.load(in);
                APP_CONFIG.putAll(defaultProps);
            } catch (Exception e) {
                System.err.println("加载配置异常:" + e.getMessage());
            }
        }
    }
}
```

**特点**:
- ✅ 类加载时自动初始化
- ✅ 从 resources/config.properties 读取默认配置
- ✅ 支持运行时动态设置（AppNginx启动时设置路径）

### 配置项定义

```java
// 配置Key常量
private static final String NGINX_STATUS_URL_KEY = "nginx_status_url";
public static final String NGINX_PATH_KEY = "nginx.path";
public static final String NGINX_CONF_PATH_KEY = "nginx.path.conf";
public static final String NGINX_CONF_BACKUPS_KEY = "nginx.path.backups";
```

### 访问方法

```java
// 获取所有配置
Properties getConfig()

// 获取指定配置项
String getConfig(String key)

// 获取Nginx状态URL（有默认值）
String getNginxStatusUrl() {
    return APP_CONFIG.getProperty(
        NGINX_STATUS_URL_KEY, 
        "http://localhost/nginx_status"
    );
}

// 获取Nginx路径
String getNginxPath() {
    return APP_CONFIG.getProperty(NGINX_PATH_KEY, "");
}

// 获取nginx.conf路径（启动后有效）
String getNginxConfPath() {
    return APP_CONFIG.getProperty(NGINX_CONF_PATH_KEY, "");
}

// 获取备份目录路径（启动后有效）
String getNginxBackupPath() {
    return APP_CONFIG.getProperty(NGINX_CONF_BACKUPS_KEY, "");
}
```

## 其他组件如何使用配置

### NginxConfigParser 使用示例

```java
public class NginxConfigParser {
    
    private static String getNginxConfPath() {
        return AppConfig.getNginxConfPath();
    }
    
    public static Map<String, String> parseConfig() throws IOException {
        String confPath = getNginxConfPath();
        String content = new String(Files.readAllBytes(Paths.get(confPath)));
        // 解析逻辑...
    }
    
    public static void updateConfig(String directive, String newValue) 
            throws IOException {
        backupConfig();  // 先备份
        String confPath = getNginxConfPath();
        // 更新逻辑...
    }
    
    public static void backupConfig() throws IOException {
        File backupDir = new File(AppConfig.getNginxBackupPath());
        // 备份逻辑...
    }
}
```

### NginxStatus 使用示例

```java
public class NginxStatus extends ModelBase implements Monitor {
    
    @Override
    public Map<String, String> monitor(Request request) {
        // 从AppConfig获取状态URL
        String statusUrl = AppConfig.getNginxStatusUrl();
        
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(statusUrl))
            .build();
        // 请求逻辑...
    }
}
```

### NginxBackup 使用示例

```java
public class NginxBackup extends ModelBase implements List, Show {
    
    @Override
    public List<String[]> list(Request request, ...) throws Exception {
        // 从AppConfig获取备份目录
        File backupDir = new File(AppConfig.getNginxBackupPath());
        File[] backups = backupDir.listFiles();
        // 列出备份文件...
    }
    
    public void restoreBackup(String fileName) throws IOException {
        File backupDir = new File(AppConfig.getNginxBackupPath());
        Path backupPath = Paths.get(backupDir.getAbsolutePath(), fileName);
        Path confPath = Paths.get(AppConfig.getNginxConfPath());
        // 恢复逻辑...
    }
}
```

## 启动日志示例

```
========================================== nginx ====================================================
nginx 应用配置 nginx_status_url=http://localhost/nginx_status
nginx 应用版本号：1.0.0-SNAPSHOT
nginx 应用根路径：/home/qingzhou/.../qingzhou-nginx-app
nginx 应用临时目录：/opt/qingzhou/instances/default/temp/nginx-app
========================================== nginx ====================================================
```

## 两种模式对比

| 特性 | 开发模式 (file) | 生产模式 (jar) |
|------|----------------|----------------|
| 检测方式 | URL协议="file" | URL协议="jar" |
| 源位置 | src/main/resources/ | JAR包内部 |
| 拷贝方式 | InputStream统一处理 | InputStream统一处理 |
| 性能 | 快 | 稍慢（需解压） |
| 可修改源文件 | 是（可直接编辑） | 否（需重新打包） |
| 代码差异 | **无** | **无** |

**核心优势**: 使用 `URL.openStream()` 统一处理，无需区分模式！

## 配置优先级

```
AppNginx.start() 设置的路径
         ↓ (覆盖)
resources/config.properties 中的默认配置
```

**示例**:
```java
// 1. config.properties 中的默认值
nginx_status_url=http://localhost/nginx_status

// 2. AppNginx.start() 动态设置
AppConfig.getConfig().setProperty("nginx.path.conf", "/tmp/.../nginx.conf");

// 3. 最终结果
AppConfig.getNginxStatusUrl()  → http://localhost/nginx_status
AppConfig.getNginxConfPath()   → /tmp/.../nginx.conf
```

## 错误处理

### 常见错误及解决方案

| 错误 | 原因 | 解决方案 |
|------|------|----------|
| 找不到nginx.conf | 资源文件未包含在构建中 | 检查pom.xml的resources配置 |
| 临时目录无权限 | 目录权限不足 | 检查轻舟平台目录权限 |
| 拷贝失败 | 文件被占用 | 关闭占用文件的进程 |
| 配置未生效 | 启动顺序问题 | 确保在AppNginx.start()之后访问 |

### 异常处理机制

```java
try {
    // 启动逻辑...
} catch (IOException | UnsupportedOperationException e) {
    System.err.println("nginx 应用启动异常: " + e.getMessage());
    // 记录异常但不阻止平台启动（优雅降级）
}
```

## 测试建议

### 开发模式测试

```bash
# 1. 编译
mvn clean compile

# 2. 运行测试
mvn test

# 3. 启动轻舟平台（开发模式）
# 验证日志输出和临时目录文件
ls -la {轻舟临时目录}/
cat {轻舟临时目录}/nginx.conf
```

### JAR模式测试

```bash
# 1. 打包
mvn clean package

# 2. 检查JAR内容
jar tf target/qingzhou-nginx-app-*.jar | grep nginx.conf

# 3. 部署到轻舟平台
# 验证从JAR解压是否正常
```

## 最佳实践

### 1. 配置管理

```java
// ✅ 推荐：通过AppConfig统一管理
String confPath = AppConfig.getNginxConfPath();

// ❌ 不推荐：硬编码路径
String confPath = "/home/qingzhou/ngx/nginx.conf";
```

### 2. 文件操作

```java
// ✅ 推荐：使用try-with-resources
try (InputStream in = url.openStream()) {
    Files.copy(in, target, REPLACE_EXISTING);
}

// ✅ 推荐：检查文件存在性
if (targetFile.exists()) {
    // 复用已存在的文件
}
```

### 3. 路径获取

```java
// ✅ 推荐：使用AppConfig
String backupPath = AppConfig.getNginxBackupPath();

// ❌ 不推荐：拼接路径
String backupPath = tempDir + "/backups";
```

### 4. 日志输出

```java
// ✅ 推荐：使用统一前缀
System.out.println("nginx 应用版本号：" + version);

// ❌ 不推荐：无前缀
System.out.println("版本号：" + version);
```

## 扩展指南

### 添加新的配置文件

如果需要添加其他配置文件（如mime.types），参照nginx.conf的处理方式：

```java
// 在AppNginx.start()中添加
Path mimeTypes = new File(appContext.getTemp(), "mime.types").toPath();
URL mimeTypesUrl = getClass().getClassLoader().getResource("mime.types");

try (InputStream in = mimeTypesUrl.openStream()) {
    Files.copy(in, mimeTypes, StandardCopyOption.REPLACE_EXISTING);
}

// 注册到AppConfig
AppConfig.getConfig().setProperty("nginx.path.mime_types", 
    mimeTypes.toString());
```

### 添加新的配置项

```java
// 1. 在AppConfig中添加Key常量
public static final String NEW_CONFIG_KEY = "nginx.new_config";

// 2. 添加getter方法
public static String getNewConfig() {
    return APP_CONFIG.getProperty(NEW_CONFIG_KEY, "default_value");
}

// 3. 在AppNginx.start()中设置（如果需要）
AppConfig.getConfig().setProperty(NEW_CONFIG_KEY, value);
```

## 架构优势

### 1. 应用隔离

每个Nginx应用实例有独立的临时目录：
```
实例1: /opt/qingzhou/instances/instance1/temp/nginx-app/
实例2: /opt/qingzhou/instances/instance2/temp/nginx-app/
```

### 2. 配置集中化

所有配置通过 AppConfig 统一管理：
- ✅ 单一数据源
- ✅ 类型安全访问
- ✅ 便于测试和调试

### 3. 自动适配

开发/生产模式自动识别：
- ✅ 无需修改代码
- ✅ 无需额外配置
- ✅ 开箱即用

### 4. 优雅降级

启动失败不阻止平台：
- ✅ 记录错误日志
- ✅ 不影响其他应用
- ✅ 便于问题排查

## 总结

AppNginx的start方法实现了：
- ✅ 目录自动创建（临时目录 + 备份目录）
- ✅ nginx.conf智能部署（支持开发/生产两种模式）
- ✅ 路径集中管理（通过AppConfig统一访问）
- ✅ 完善的日志输出（便于调试）
- ✅ 优雅的错误处理（不阻止平台启动）

整个流程自动化、透明化，其他组件只需通过 `AppConfig` 即可访问所有配置和路径！
