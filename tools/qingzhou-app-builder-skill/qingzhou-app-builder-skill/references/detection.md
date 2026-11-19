# 自动发现

轻舟应用用于管理目标软件，实施时两者通常独立部署，应用需要等待目标软件就绪后再工作。为此应用可在 `META-INF/MANIFEST.MF` 中配置 `Qingzhou-Detection-*`，由引擎每 5 秒探测一次：所有命中的安装目录各启动一个应用 bundle 实例（应用 code 追加 `:<序号>`，`AppContext.getDetectedPath()` 返回各自目录）；全部未命中时停止该 bundle，无需人工干预。未启用时应用按常规方式随轻舟启动。

## 配置方式

在 Maven 项目的 `pom.xml` 中通过 `maven-jar-plugin` 的 `manifestEntries` 注入，打包时写入 `META-INF/MANIFEST.MF`：

```xml
<plugin>
    <artifactId>maven-jar-plugin</artifactId>
    <configuration>
        <archive>
            <manifestEntries>
                <Qingzhou-Detection-Enabled>true</Qingzhou-Detection-Enabled>
                <Qingzhou-Detection-Feature-Files>bin/startup.sh,conf/config.xml</Qingzhou-Detection-Feature-Files>
                <Qingzhou-Detection-Env-Vars>DEMO_HOME</Qingzhou-Detection-Env-Vars>
                <Qingzhou-Detection-Scan-Roots>user.home,/usr/local,/opt</Qingzhou-Detection-Scan-Roots>
                <Qingzhou-Detection-Process-Names>com.example.demo.Server,demo.jar</Qingzhou-Detection-Process-Names>
                <Qingzhou-Detection-Process-Home-Regex>-Ddemo\.home=(\S+)</Qingzhou-Detection-Process-Home-Regex>
            </manifestEntries>
        </archive>
    </configuration>
</plugin>
```

### 示例：Tomcat

```xml
<manifestEntries>
    <Qingzhou-Detection-Enabled>true</Qingzhou-Detection-Enabled>
    <Qingzhou-Detection-Feature-Files>bin/catalina.sh,bin/catalina.bat,conf/server.xml
    </Qingzhou-Detection-Feature-Files>
    <Qingzhou-Detection-Env-Vars>CATALINA_HOME,TOMCAT_HOME</Qingzhou-Detection-Env-Vars>
    <Qingzhou-Detection-Scan-Roots>user.home,/usr/local,/opt</Qingzhou-Detection-Scan-Roots>
    <Qingzhou-Detection-Process-Names>org.apache.catalina.startup.Bootstrap,bootstrap.jar
    </Qingzhou-Detection-Process-Names>
    <Qingzhou-Detection-Process-Home-Regex>-Dcatalina\.home=(\S+)</Qingzhou-Detection-Process-Home-Regex>
</manifestEntries>
```

Tomcat 以 `java -Dcatalina.home=... org.apache.catalina.startup.Bootstrap` 启动，关键字前缀是 JDK 路径而非安装目录，因此用 `Process-Home-Regex` 从 `-Dcatalina.home=` 提取安装目录。

## 参数详解

| 参数名                                     | 必填 | 默认值     | 说明                                                                                                                                                                     |
|-----------------------------------------|----|---------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Qingzhou-Detection-Enabled`            | 否  | `false` | 是否启用自动发现。未启用时后续参数全部无效。                                                                                                                                                 |
| `Qingzhou-Detection-Feature-Files`      | 是  | -       | 特征文件列表，逗号分隔，相对候选安装目录的**相对路径**；命中任一即收集该候选目录，返回全部命中目录。为空则永不命中。每个命中目录各启动一个应用实例，应用通过 `AppContext.getDetectedPath()` 获取对应目录。                                                |
| `Qingzhou-Detection-Env-Vars`           | 否  | -       | 环境变量名列表，逗号分隔。变量值**直接作为**安装目录（不再下钻子目录），最先尝试，如 `JAVA_HOME`、`M2_HOME`。                                                                                                    |
| `Qingzhou-Detection-Scan-Roots`         | 否  | -       | 扫描根路径列表，逗号分隔，仅扫描根目录及其一级子目录。整项等于 `user.home` 时替换为当前用户主目录（不做字符串内嵌替换）。清单未配置时回落到系统属性同名项。                                                                                   |
| `Qingzhou-Detection-Process-Names`      | 否  | -       | 命令行关键字列表，逗号分隔，小写匹配。命中后默认取关键字**之前的命令行片段**作为候选安装目录（关键字位于行首则丢弃）；仅在前两种策略均未命中时启用。对于以 `java -D... MainClass` 启动的 JVM 进程，关键字前缀通常是 JDK 路径而非目标安装目录，须配合 `Process-Home-Regex` 提取。 |
| `Qingzhou-Detection-Process-Home-Regex` | 否  | -       | 从命中进程的命令行提取安装目录的**正则表达式**，须含一个捕获组。配了则优先用捕获组取值，取不到再退回关键字前缀。用于 JVM 进程从 `-Dxxx.home=` / `-Dxxx.base=` 等系统属性中定位安装目录，如 Tomcat 配 `-Dcatalina\.home=(\S+)`。                   |

## 探测流程

1. `Env-Vars`：变量值即候选目录。
2. `Scan-Roots`：根目录及其一级子目录作为候选目录。
3. `Process-Names`：从运行中的进程命令行提取候选目录（Windows 用 PowerShell 取 Path，其他平台用 `ps -ww -e -o args=`）。命中关键字后，若配置了 `Process-Home-Regex` 则用其捕获组取目录，否则取关键字之前的命令行片段。
4. 每个候选目录按 `Feature-Files` 校验，命中任一特征文件即收集；探测返回全部命中目录，逐个启动应用实例。

## 获取探测结果

```java
public class MyApp implements QingzhouApp {
    @Override
    public void start(AppContext context) {
        String detectedPath = context.getDetectedPath();
        if (detectedPath != null) {
            // 目标软件已发现，执行初始化逻辑
        } else {
            // 尚未发现：保持等待，不要抛异常终止应用，引擎会在下次探测命中后重新启动
        }
    }
}
```

## 最佳实践

1. **特征文件选择**：选择安装目录下**唯一且稳定**的文件，如 `bin/startup.sh`、`lib/core.jar`。
2. **环境变量优先**：对通过环境变量安装的软件（JDK、Maven 等）优先配置 `Env-Vars`，可显著提高探测效率。
3. **扫描路径精简**：避免过深过广的扫描根，建议限定 `/opt`、`/usr/local` 等常见安装目录。
4. **进程关键字精确**：`Process-Names` 取的是命令行中的路径片段，应使用安装目录内可识别的稳定片段，避免用过于宽泛的单词。
5. **跨平台适配**：通过多个特征文件（如 `bin/catalina.sh`、`bin/catalina.bat`）支持多平台部署。
