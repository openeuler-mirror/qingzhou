package qingzhou.app.tomcat;

import qingzhou.api.*;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.List;

@Model(code = "applications", order = 1, icon = "ShoppingBag",
        name = {"应用管理", "en:Applications"},
        info = {"从server.xml解析并管理Tomcat应用(Context)", "en:Parse and manage Tomcat applications from server.xml"})
public class TomcatApplications extends TomcatModelBase implements qingzhou.api.type.List {
    @ModelField(
            id = true,
            name = {"应用名称", "en:App Name"},
            info = {"应用名称，如 demo", "en:App name, e.g. demo"},
            list = true, add = false, update = false, show = true, readonly = true)
    public String name;

    @ModelField(
            name = {"上下文路径", "en:Context Path"},
            info = {"应用访问路径，如 /demo", "en:Application access path, e.g. /demo"},
            list = true, show = true, required = true)
    public String contextPath;

    @ModelField(
            name = {"文档基目录", "en:DocBase"},
            info = {"应用文档基目录", "en:Application document base directory"},
            list = true,
            add = true, update = true, required = true)
    public String docBase;

    @ModelField(
            name = {"可重载", "en:Reloadable"},
            info = {"是否自动重载", "en:Auto reload enabled"},
            list = true,
            input_type = InputType.select,
            options = {"true", "false"},
            add = true, update = true)
    public String reloadable;

    @ModelField(
            name = {"应用状态", "en:Status"},
            info = {"应用运行状态", "en:Application status"},
            list = true,
            show = true,
            readonly = true)
    public String status;

    private List<Map<String, String>> applications;

    @Override
    public List<String[]> list(int pageNum, int pageSize,
                               Map<String, String> query, String[] listFields) throws Exception {
        loadApplications();
        List<Map<String, String>> filtered = filterByQuery(applications, query);
        return buildListResult(filtered, pageNum, pageSize, listFields);
    }

    @Override
    public int totalSize(Map<String, String> query) {
        loadApplications();
        return filterByQuery(applications, query).size();
    }

    @Override
    public boolean contains(String id) {
        loadApplications();
        return applications.stream().anyMatch(a -> a.get("name").equals(id));
    }

    private void loadApplications() {
        applications = new ArrayList<>();

        File contextXml = getContextXmlFile();
        if (contextXml == null || !Files.exists(contextXml.toPath())) {
            return;
        }
        String appBase = getAppBase();
        if (appBase == null) {
            return;
        }

        List<Properties> contexts;
        try {
            contexts = getXmlNodes(contextXml, "//Context/Context");
        } catch (Exception e) {
            return;
        }

        for (Properties context : contexts) {
            Map<String, String> app = new LinkedHashMap<>();

            String path = context.getProperty("path", "");
            String docBase = context.getProperty("docBase", "");
            String reloadableVal = context.getProperty("reloadable", "false");

            // 从 path 属性提取应用名称（去掉开头的 /）
            String appName = extractAppNameFromPath(path);

            app.put("name", appName);
            app.put("contextPath", path);
            app.put("docBase", docBase);
            app.put("reloadable", reloadableVal);

            String fullPath = resolveFullPath(appBase, docBase);
            Path appDir = Paths.get(fullPath);
            boolean dirExists = Files.exists(appDir);
            app.put("exists", dirExists ? "是" : "否");

            app.put("status", dirExists ? "deployed" : "not_found");

            applications.add(app);
        }

        File webappsDir = new File(getTomcatBaseDir(), "webapps");

        if (!webappsDir.exists() || !webappsDir.isDirectory()) {
            return;
        }

        // 构建已存在应用名称的 Set，提高查找效率 (O(1) vs O(n))
        Set<String> existingAppNames = new HashSet<>();
        for (Map<String, String> app : applications) {
            String appName = app.get("name");
            if (appName != null && !appName.isEmpty()) {
                existingAppNames.add(appName);
            }
        }

        File[] files = webappsDir.listFiles();
        if (files == null) {
            return;
        }

        // 遍历 webapps 目录下的文件
        for (File file : files) {
            String fileName = file.getName();

            // 跳过已注册的应用
            if (existingAppNames.contains(fileName)) {
                continue;
            }

            // 跳过非应用文件（如 .txt, .xml 等）
            if (isNonApplicationFile(file)) {
                continue;
            }

            // 从文件名提取应用信息
            String appName = extractAppNameFromPath(fileName);
            String contextPath = "/" + appName;
            String docBase = file.getName();

            // 创建应用配置
            Map<String, String> appProps = new LinkedHashMap<>();
            appProps.put("name", appName);
            appProps.put("contextPath", contextPath);
            appProps.put("docBase", docBase);
            appProps.put("reloadable", "true");  // 默认启用热加载
            appProps.put("status", file.isDirectory() ? "deployed" : "not_found");
            applications.add(appProps);
        }
    }

    /**
     * 判断是否为非应用文件
     */
    private boolean isNonApplicationFile(File file) {
        String name = file.getName().toLowerCase();
        // 跳过隐藏文件、配置文件、临时文件等
        return name.startsWith(".") ||
                name.endsWith(".xml") ||
                name.endsWith(".txt") ||
                name.equals("work") ||
                name.equals("temp") ||
                name.endsWith(".war");
    }

    /**
     * 从 path 属性提取应用名称（去掉开头的 /）
     * 例如：path="/manager" → 应用名 "manager"
     */
    private String extractAppNameFromPath(String path) {
        if (path == null || path.isEmpty()) {
            return "ROOT";
        }
        // 去掉开头的 /
        String appName = path.trim();
        if (appName.startsWith("/")) {
            appName = appName.substring(1);
        }
        // 如果为空（path="" 或 path="/"），表示 ROOT 应用
        if (appName.isEmpty()) {
            appName = "ROOT";
        }
        return appName;
    }

    private String getAppBase() {
        File serverXml = getServerXmlFile();

        Map<String, String> hostConfig = new HashMap<>();
        if (serverXml.exists()) {
            try {
                Properties host = getXmlNode(serverXml, "//Engine/Host");
                if (host != null) {
                    hostConfig = toMap(host);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            String appBase = hostConfig.get("appBase");
            if (appBase != null && !appBase.isEmpty()) {
                if (!appBase.startsWith("/") && !appBase.contains(":")) {
                    appBase = serverXml.getParentFile().getParent() + "/" + appBase;
                }
                return appBase;
            }
        }
        return null;
    }

    private String resolveFullPath(String appBase, String docBase) {
        if (docBase.startsWith("/") || docBase.contains(":")) {
            return docBase;
        }
        return resolvePath(appBase, docBase).toString();
    }
}
