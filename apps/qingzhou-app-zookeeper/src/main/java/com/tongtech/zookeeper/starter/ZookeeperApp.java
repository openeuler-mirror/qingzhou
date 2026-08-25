package com.tongtech.zookeeper.starter;

import com.tongtech.zookeeper.starter.util.FileUtil;
import com.tongtech.zookeeper.starter.util.Util;
import com.tongtech.zookeeper.starter.util.ZkJaaSConfigExtractor;
import com.tongtech.zookeeper.starter.util.ZkPathExtractor;
import qingzhou.api.App;
import qingzhou.api.AppContext;
import qingzhou.api.Menu;
import qingzhou.api.QingzhouApp;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Request;
import qingzhou.http.client.Response;
import qingzhou.json.Json;
import qingzhou.logger.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Zookeeper 应用入口：启动时探测安装路径、加载配置并初始化监控元数据。
 */
@App(icon = "Cloud",
        name = {"Zookeeper应用", "en:Zookeeper Application"},
        info = {"用于演示Zookeeper的核心功能，包括服务注册发现、配置管理等。", "en:Zookeeper application demo with service discovery and configuration management."})
@Menu(name = {"概要", "en:Summary"}, code = "summary", icon = "House", order = 1)
@Menu(name = {"服务状态", "en:Server stats"}, code = "serverstats", icon = "icon-bar-chart-alt", order = 2)
@Menu(name = {"连接", "en:Connections"}, code = "connections", icon = "icon-bar-chart-alt", order = 3)
@Menu(name = {"生效参数", "en:Validity parameter"}, code = "configuration", icon = "icon-bar-chart-alt", order = 4)
@Menu(name = {"配置", "en:config"}, code = "config", icon = "icon-bar-chart-alt", order = 5)
public class ZookeeperApp implements QingzhouApp {
    public static final String METADATA_KEY = "metadata";
    public static final String METADATA_CONFIG_KEY = "metadata_zoo";
    public static final String METADATA_MYID_KEY = "metadata_myid";
    public static final String METADATA_INSTALL_KEY = "metadata_install";
    public static final String METADATA_VERSION_KEY = "metadata_version";
    public static final String METADATA_JAAS_KEY = "metadata_jaas_file";
    public static final String METADATA_COMMANDS_KEY = "metadata_commands";

    public static final String ZOO_KEY = "zoo";
    public static final String ZOO_DATADIR_KEY = "dataDir";
    public static final String MYID_KEY = "myid";

    public static final String MAIN = "QuorumPeerMain";

    public static final String ZOO_FILE_NAME = "zoo.cfg";
    public static final String MYID_FILE_NAME = "myid";
    public static Logger logger;
    public static HttpClient httpClient;
    public static Json json;
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");
    private static final boolean IS_LINUX = System.getProperty("os.name").toLowerCase().contains("linux");

    @Override
    public void start(AppContext appContext) throws Exception {
        logger = appContext.getService(Logger.class);
        httpClient = appContext.getService(HttpClient.class);
        json = appContext.getService(Json.class);
        String detectionPath = appContext.getDetectedPath();
        logger.info("zk install path：" + detectionPath);
        String zooFilePath = "";
        String zkCmd = findZK();
        if (Util.notBlank(zkCmd)) {
            String installPath = ZkPathExtractor.extractInstallPathFromJar(zkCmd);
            if (Util.notBlank(installPath)) {
                if (!installPath.equals(detectionPath)) {
                    throw new RuntimeException("Found differences in zk：" + installPath);
                }
            }
            String cfgFile = ZkPathExtractor.extractConfigPath(zkCmd);
            if (Util.notBlank(cfgFile)) {
                zooFilePath = cfgFile;
            }
        }

        if (Util.isBlank(zooFilePath)) {
            if (Util.isBlank(detectionPath)) {
                logger.info("The installation path for zk has not been configured.");
                return;
            }
            File file = FileUtil.newFile(detectionPath, "conf", ZOO_FILE_NAME);
            if (!file.exists()) {
                throw new RuntimeException("Failed to find zoo.cfg");
            }
            zooFilePath = file.getAbsolutePath();
        }
        Properties metadata = new Properties();
        metadata.setProperty(METADATA_INSTALL_KEY, detectionPath);
        metadata.setProperty(METADATA_CONFIG_KEY, zooFilePath);

        Properties properties = loadZooFile(zooFilePath);
        loadMyId(appContext, properties, metadata);
        loadAuth(properties, metadata, zkCmd);
        String version = getVersion(detectionPath, properties, metadata);
        metadata.setProperty(METADATA_VERSION_KEY, version);
        appContext.getProperties().put(ZOO_KEY, properties);
        appContext.getProperties().put(METADATA_KEY, metadata);
    }

    private static void loadAuth(Properties properties, Properties metadata, String zkCmd) throws IOException {
        boolean enableAuth = false;
        for (Object value : properties.values()) {
            if ("org.apache.zookeeper.server.auth.SASLAuthenticationProvider".equals(value)) {
                enableAuth = true;
                break;
            }
        }
        if (!enableAuth) {
            return;
        }

        // 优先从脚本配置文件获取，再从启动命令中获取
        String home = metadata.getProperty(METADATA_INSTALL_KEY);
        String jaasFilePath = extractFromScript(home, IS_WINDOWS ? "zkEnv.cmd" : "zkEnv.sh");
        String value = extractFromScript(home, IS_WINDOWS ? "zkServer.cmd" : "zkServer.sh");
        if (Util.notBlank(value)) {
            jaasFilePath = value;
        }

        if (Util.notBlank(zkCmd) && zkCmd.contains("-Djava.security.auth.login.config")) {
            value = ZkJaaSConfigExtractor.extractJaaSPathFromString(zkCmd);
            if (Util.notBlank(value)) {
                jaasFilePath = value;
            }
        }

        if (Util.notBlank(jaasFilePath) && FileUtil.newFile(jaasFilePath).exists()) {
            metadata.setProperty(METADATA_JAAS_KEY, jaasFilePath);
        }
    }

    private static String extractFromScript(String home, String scriptName) {
        return ZkJaaSConfigExtractor.extractFromScript(FileUtil.newFile(home, "bin", scriptName).toPath());
    }

    private static void loadMyId(AppContext appContext, Properties properties, Properties metadata) throws IOException {
        String dataDirPath = properties.getProperty(ZOO_DATADIR_KEY);
        File myidFile = FileUtil.newFile(dataDirPath, MYID_FILE_NAME);
        if (myidFile.exists()) {
            metadata.setProperty(METADATA_MYID_KEY, myidFile.getAbsolutePath());
            String first = FileUtil.readLines(myidFile).get(0);
            Properties myid = new Properties();
            myid.setProperty(first, first);
            appContext.getProperties().put(MYID_KEY, myid);
        }
    }

    private Properties loadZooFile(String zooFilePath) throws IOException {
        Properties cfg = new Properties();
        Path path = Paths.get(zooFilePath);
        if (path.toFile().exists()) {
            try (InputStream input = Files.newInputStream(path)) {
                cfg.load(input);
            }
        }
        return cfg;
    }

    private String findZK() {
        String[] cmd;
        if (IS_WINDOWS) {
            // Windows: 使用 wmic 获取所有进程的完整命令行（含 -cp / -classpath 等参数）
            cmd = new String[]{"cmd", "/c", "wmic process where \"CommandLine is not null\" get CommandLine /format:list"};
        } else {
            // Linux & Mac: ps 输出完整命令行
            // -ww 避免 macOS BSD ps 按终端宽度截断长命令行；Linux procps 亦兼容
            cmd = new String[]{"sh", "-c", "ps -ww -e -o args="};
        }

        String zkCmd = runAndFind(cmd);
        if (zkCmd == null && IS_LINUX) {
            // Linux 兜底：ps 不可用或输出异常时，直接扫描 /proc/<pid>/cmdline
            zkCmd = findZkFromProc();
        }
        return zkCmd == null ? "" : zkCmd;
    }

    /** 执行命令并在输出中查找 ZooKeeper 主进程（QuorumPeerMain）命令行 */
    private String runAndFind(String[] cmd) {
        Process process = null;
        try {
            process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                return reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && line.toLowerCase().contains(MAIN.toLowerCase()))
                        .findFirst().orElse(null);
            }
        } catch (Exception e) {
            logger.error("findZk error", e);
            return null;
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }
    }

    /** Linux 兜底：直接读取 /proc/<pid>/cmdline（参数以 \0 分隔，需替换为空格） */
    private String findZkFromProc() {
        File proc = new File("/proc");
        File[] pidDirs = proc.listFiles(f -> f.isDirectory() && isNumeric(f.getName()));
        if (pidDirs == null) {
            return null;
        }
        for (File pidDir : pidDirs) {
            try {
                byte[] bytes = Files.readAllBytes(new File(pidDir, "cmdline").toPath());
                String line = new String(bytes, StandardCharsets.UTF_8).replace('\0', ' ').trim();
                if (line.toLowerCase().contains(MAIN.toLowerCase())) {
                    return line;
                }
            } catch (Exception ignored) {
                // 权限不足或进程已退出，跳过
            }
        }
        return null;
    }

    private static boolean isNumeric(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private String getVersion(String installPath, Properties cfg, Properties metadata) {
        String version = getZKVersionByInstall(installPath);
        String enableServer = cfg.getProperty("admin.enableServer", System.getProperty("zookeeper.admin.enableServer", "true"));
        if (Util.comparativeVersion(version) && Boolean.parseBoolean(enableServer)) {
            String url = getCommandsUrl(cfg);
            metadata.put(METADATA_COMMANDS_KEY, url);
            String environmentUrl = url + "/environment";
            try {
                Map<String, String> headMap = new HashMap<>();
                headMap.put("Content-Type", "application/json");
                headMap.put("accept", "application/json");
                Request request = httpClient.newRequest(environmentUrl)
                        .method(HttpMethod.GET).headers(headMap);
                Response response = httpClient.send(request);
                if (200 != response.getStatus() || response.getBody() == null) {
                    return version;
                }
                Map<String, String> environment = json.fromJson(new String(response.getBody(), StandardCharsets.UTF_8), HashMap.class);
                String zkVersion = environment.get("zookeeper.version");
                if (!Util.isBlank(zkVersion)) {
                    version = zkVersion.contains("-") ? zkVersion.split("-")[0] : zkVersion;
                }
            } catch (Exception e) {
                if (logger.isWarnEnabled()) {
                    logger.warn(environmentUrl, e);
                }
            }
        }
        return version;
    }

    private static String getCommandsUrl(Properties cfg) {
        String serverPort = cfg.getProperty("admin.serverPort", System.getProperty("zookeeper.admin.serverPort", "8080"));
        String commandUrl = cfg.getProperty("admin.commandURL", System.getProperty("zookeeper.admin.commandURL", "/commands"));
        return "http://localhost:" + serverPort + commandUrl;
    }

    private String getZKVersionByInstall(String installPath) {
        File lib = FileUtil.newFile(installPath, "lib");
        if (!lib.exists()) {
            throw new RuntimeException("The zookeeper lib directory does not exist");
        }
        File[] files = lib.listFiles((dir, name) ->
                name.endsWith(".jar") && name.startsWith("zookeeper-") && name.split("-").length == 2);
        if (files == null || files.length != 1) {
            throw new RuntimeException("zookeeper-version.jar not found");
        }
        String[] split = files[0].getName().split("-");
        return split[1].split("\\.jar")[0];
    }

    public static String sendHttp(Request request) throws Exception {
        Response response = httpClient.send(request);
        if (200 == response.getStatus()) {
            return new String(response.getBody(), StandardCharsets.UTF_8);
        }
        return "";
    }

    @Override
    public void stop() {
        httpClient = null;
        json = null;
    }
}
