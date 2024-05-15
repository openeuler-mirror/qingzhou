package qingzhou.app.master.service;

import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.api.type.Createable;
import qingzhou.api.type.Deletable;
import qingzhou.api.type.Listable;
import qingzhou.app.master.MasterApp;
import qingzhou.console.RequestImpl;
import qingzhou.deployer.Deployer;
import qingzhou.engine.util.Utils;
import qingzhou.registry.AppInfo;
import qingzhou.registry.InstanceInfo;
import qingzhou.registry.Registry;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Model(code = "app", icon = "cube-alt",
        menu = "Service", order = 1,
        name = {"应用", "en:App"},
        info = {"应用。",
                "en:App Management."})
public class App extends ModelBase implements Createable {
    @ModelField(
            list = true,
            name = {"名称", "en:Name"},
            info = {"应用名称。", "en:App Name"})
    public String id;

    @ModelField(
            type = FieldType.bool,
            name = {"使用上传", "en:Enable Upload"},
            info = {"安装的应用可以从客户端上传，也可以从服务器端指定的位置读取。",
                    "en:The installed app can be uploaded from the client or read from a location specified on the server side."})
    public boolean appFrom = false;

    @ModelField(
            list = true, show = "appFrom=false",
            name = {"应用位置", "en:Application File"},
            info = {"服务器上应用程序的位置，通常是应用的程序包，注：须为 *.jar 类型的文件。",
                    "en:The location of the application on the server, usually the app package, Note: Must be a *.jar file."})
    public String filename;

    @ModelField(
            type = FieldType.file, show = "appFrom=true",
            name = {"上传应用", "en:Upload Application"},
            info = {"上传一个应用文件到服务器，文件须是 *.jar 或 *.zip 类型的 Qingzhou 应用文件，否则可能会导致安装失败。",
                    "en:Upload an application file to the server, the file must be a *.jar type qingzhou application file, otherwise the installation may fail."})
    public String fromUpload;

    @ModelField(
            type = FieldType.select,
            list = true,
            name = {"节点", "en:Node"},
            info = {"选择安装应用的节点。", "en:Select the node where you want to install the application."})
    public String instances;

    @Override
    public void start() {
        appContext.addI18n("app.id.not.exist", new String[]{"应用文件不存在", "en:The app file does not exist"});
        appContext.addI18n("app.type.unknown", new String[]{"未知的应用类型", "en:Unknown app type"});
    }

    @ModelAction(
            name = {"列表", "en:List"}, forward = "list",
            info = {"展示该类型的所有组件数据或界面。", "en:Show all component data or interfaces of this type."})
    public void list(Request request, Response response) throws Exception {
        int pageNum = 1;
        try {
            pageNum = Integer.parseInt(request.getParameter(Listable.PARAMETER_PAGE_NUM));
        } catch (NumberFormatException ignored) {
        }

        int pageSize = response.getPageSize();
        if (pageSize == -1) {
            pageSize = 10;
        }

        int totalSize = 0;
        Map<String, List<Map<String, String>>> appNameMap = new HashMap<>();
        Collection<String> localAppNames = MasterApp.getService(Deployer.class).getAllApp();
        // 处理本地应用名称
        if (!localAppNames.isEmpty()) {
            appNameMap.put(MasterApp.getInstanceId(), localAppNames.stream().map(appName -> {
                Map<String, String> appMap = new HashMap<>();
                appMap.put("id", appName);
                appMap.put("instances", MasterApp.getInstanceId());
                appMap.put("filename", ""); // ToDo
                return appMap;
            }).collect(Collectors.toList()));
            totalSize += localAppNames.size();
        }

        try {
            Registry registry = MasterApp.getService(Registry.class);
            Collection<String> allInstanceIds = registry.getAllInstanceId();
            // 处理远程实例的应用信息
            for (String instanceId : allInstanceIds) {
                InstanceInfo instanceInfo = registry.getInstanceInfo(instanceId);
                AppInfo[] appInfos = instanceInfo.getAppInfos();
                appNameMap.put(instanceId, Arrays.stream(appInfos).map(appInfo -> {
                    Map<String, String> appMap = new HashMap<>();
                    appMap.put("id", appInfo.getName());
                    appMap.put("instances", instanceId);
                    appMap.put("filename", ""); // ToDo
                    return appMap;
                }).collect(Collectors.toList()));
                totalSize += appInfos.length;
            }
        } catch (Exception ignored) {
        }

        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = startIndex + pageSize;

        for (List<Map<String, String>> apps : appNameMap.values()) {
            if (apps.size() > startIndex) {
                for (int i = startIndex; i < Math.min(endIndex, apps.size()); i++) {
                    response.addData(apps.get(i));
                }
            }
        }

        response.setTotalSize(totalSize);
        response.setPageSize(pageSize);
        response.setPageNum(pageNum);
    }

    @ModelAction(
            name = {"安装", "en:Install"},
            info = {"按配置要求安装应用到指定的节点。", "en:Install the app to the specified node as required."})
    public void add(Request request, Response response) throws Exception {
        File srcFile;
        if (Boolean.parseBoolean(request.getParameter("appFrom"))) {
            srcFile = Utils.newFile(request.getParameter("fromUpload"));
        } else {
            srcFile = new File(request.getParameter("filename"));
        }
        if (!srcFile.isFile()) {
            response.setSuccess(false);
            String msg = this.appContext.getI18n(request.getLang(), "app.id.not.exist");
            response.setMsg(msg);
            return;
        }
        String srcFileName = srcFile.getName();
        String appName;
        if (srcFile.isDirectory()) {
            appName = srcFileName;
        } else if (srcFileName.toLowerCase().endsWith(".jar") || srcFileName.toLowerCase().endsWith(".zip")) {
            int index = srcFileName.lastIndexOf(".");
            appName = srcFileName.substring(0, index);
        } else {
            response.setSuccess(false);
            String msg = this.appContext.getI18n(request.getLang(), "app.type.unknown");
            response.setMsg(msg);
            return;
        }

        String[] nodes = request.getParameter("nodes") != null
                ? request.getParameter("nodes").split(",")
                : new String[0];
        ((RequestImpl) request).setModelName("appinstaller");
        ((RequestImpl) request).setActionName("install");
        try {
            for (String node : nodes) {
                try {
                    if ("local".equals(node)) { // 安装到本地节点
                        MasterApp.getService(Deployer.class).getApp("instance").invokeDirectly(request, response);
                    } else {
                        // TODO：调用远端 node 上的app add
                    }
                } catch (Exception e) { // todo 部分失败，如何显示到页面？
                    response.setSuccess(false);
                    response.setMsg(e.getMessage());
                    return;
                }
            }
        } finally {
            ((RequestImpl) request).setModelName("app");
            ((RequestImpl) request).setActionName(Createable.ACTION_NAME_ADD);
        }
    }

    @ModelAction(
            name = {"管理", "en:Manage"}, forward = "sys/manage",
            info = {"转到此应用的管理页面。", "en:Go to the administration page for this app."})
    public void switchTarget(Request request, Response response) throws Exception {
    }

    @ModelAction(
            batch = true,
            name = {"删除", "en:Delete"},
            info = {"删除这个组件，该组件引用的其它组件不会被删除。注：请谨慎操作，删除后不可恢复。",
                    "en:Delete this component, other components referenced by this component will not be deleted. Note: Please operate with caution, it cannot be recovered after deletion."})
    public void delete(Request request, Response response) throws Exception {
        String appName = request.getId();
        Map<String, String> p = getDataStore().getDataById("app", appName);
        String[] nodes = p.get("nodes").split(",");

        ((RequestImpl) request).setAppName("instance");
        ((RequestImpl) request).setModelName("appinstaller");
        ((RequestImpl) request).setActionName("uninstall");
        try {
            for (String node : nodes) {
                try {
                    if ("local".equals(node)) { // 安装到本地节点
                        MasterApp.getService(Deployer.class).getApp("instance").invokeDirectly(request, response);
                    } else {
                        // TODO：调用远端 node 上的app delete
                    }
                } catch (Exception e) { // todo 部分失败，如何显示到页面？
                    response.setSuccess(false);
                    response.setMsg(e.getMessage());
                }
            }
        } finally {
            ((RequestImpl) request).setAppName("master");
            ((RequestImpl) request).setModelName("app");
            ((RequestImpl) request).setActionName(Deletable.ACTION_NAME_DELETE);
        }
        getDataStore().deleteDataById("app", appName);
    }
}
