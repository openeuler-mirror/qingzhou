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
import qingzhou.api.type.Editable;
import qingzhou.api.type.Listable;
import qingzhou.app.master.MasterApp;
import qingzhou.console.RequestImpl;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.DeployerConstants;
import qingzhou.registry.AppInfo;
import qingzhou.registry.InstanceInfo;
import qingzhou.registry.Registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Model(code = DeployerConstants.MASTER_APP_APP_MODEL_NAME, icon = "cube-alt",
        menu = "Service", order = 1,
        name = {"应用", "en:App"},
        info = {"应用。",
                "en:App Management."})
public class App extends ModelBase implements Createable {
    @ModelField(
            list = true, editable = false, createable = false,
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
            info = {"服务器上应用程序的位置，通常是应用的程序包，注：须为 *.jar, *.zip 类型的文件或目录。",
                    "en:The location of the application on the server, usually the app package, Note: Must be a *.jar, *.zip file or directory."})
    public String filename;

    @ModelField(
            type = FieldType.file, show = "appFrom=true",
            name = {"上传应用", "en:Upload Application"},
            info = {"上传一个应用文件到服务器，文件须是 *.jar 或 *.zip 类型的 Qingzhou 应用文件，否则可能会导致安装失败。",
                    "en:Upload an application file to the server, the file must be a *.jar type qingzhou application file, otherwise the installation may fail."})
    public String fromUpload;

    @ModelField(
            type = FieldType.multiselect, createable = false, editable = false,// TODO 暂时不支持远程实例部署
            list = true, refModel = Instance.class,
            name = {"实例", "en:Instance"},
            info = {"选择安装应用的实例。", "en:Select the instance where you want to install the application."})
    public String instances;

    @Override
    public void start() {
        appContext.addI18n("app.id.not.exist", new String[]{"应用文件不存在", "en:The app file does not exist"});
        appContext.addI18n("app.type.unknown", new String[]{"未知的应用类型", "en:Unknown app type"});

        actionFilters.add((request, response) -> {
            if (request.getId().equals(DeployerConstants.MASTER_APP_NAME)
                    || request.getId().equals(DeployerConstants.INSTANCE_APP_NAME)) {
                if (Editable.ACTION_NAME_UPDATE.equals(request.getAction())
                        || Deletable.ACTION_NAME_DELETE.equals(request.getAction())) {
                    return appContext.getI18n(request.getLang(), "validator.master.system");
                }
            }
            return null;
        });
    }

    @ModelAction(
            name = {"查看", "en:Show"},
            info = {"查看该组件的相关信息。", "en:View the information of this model."})
    public void show(Request request, Response response) throws Exception {
        Map<String, String> appMap = new HashMap<>();
        String id = request.getId();
        qingzhou.deployer.App app = MasterApp.getService(Deployer.class).getApp(id);
        if (app != null) {
            appMap.put("id", id);
            appMap.put("instances", DeployerConstants.MASTER_APP_DEFAULT_INSTANCE_ID);
            appMap.put("filename", ""); // ToDo
            response.addData(appMap);
            return;
        }

        try {
            Registry registry = MasterApp.getService(Registry.class);
            Collection<String> allInstanceIds = registry.getAllInstanceId();
            // 处理远程实例的应用信息
            for (String instanceId : allInstanceIds) {
                InstanceInfo instanceInfo = registry.getInstanceInfo(instanceId);
                AppInfo[] appInfos = instanceInfo.getAppInfos();
                for (AppInfo appInfo : appInfos) {
                    if (id.equals(appInfo.getName())) {
                        appMap.put("id", appInfo.getName());
                        appMap.put("instances", instanceId);
                        appMap.put("filename", ""); // ToDo
                        response.addData(appMap);
                        break;
                    }
                }
                if (!appMap.isEmpty()) {
                    break;
                }
            }
        } catch (Exception ignored) {
        }
    }

    @ModelAction(
            name = {"编辑", "en:Edit"},
            info = {"获得可编辑的数据或界面。", "en:Get editable data or interfaces."})
    public void edit(Request request, Response response) throws Exception {
        show(request, response);
    }

    @ModelAction(
            name = {"更新", "en:Update"},
            info = {"更新这个模块的配置信息。", "en:Update the configuration information for this module."})
    public void update(Request request, Response response) throws Exception {
        try {
            delete(request, response);
            add(request, response);
        } finally {
            ((RequestImpl) request).setManageType(DeployerConstants.MANAGE_TYPE_APP);
            ((RequestImpl) request).setAppName(DeployerConstants.MASTER_APP_NAME);
            ((RequestImpl) request).setModelName(DeployerConstants.MASTER_APP_APP_MODEL_NAME);
            ((RequestImpl) request).setActionName(Editable.ACTION_NAME_UPDATE);
        }
    }

    @ModelAction(
            name = {"列表", "en:List"},
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

        Deployer deployer = MasterApp.getService(Deployer.class);
        Collection<String> localAppNames = deployer.getAllApp();
        Map<String, Set<String>> uniqueApps = new HashMap<>();

        // 处理本地应用名称
        for (String appName : localAppNames) {
            if (DeployerConstants.MASTER_APP_NAME.equals(appName) || DeployerConstants.INSTANCE_APP_NAME.equals(appName)) {
                continue;
            }
            uniqueApps.computeIfAbsent(appName, k -> new HashSet<>()).add(DeployerConstants.MASTER_APP_DEFAULT_INSTANCE_ID);
        }

        try {
            // 处理远程实例的应用信息
            Registry registry = MasterApp.getService(Registry.class);
            for (String instanceId : registry.getAllInstanceId()) {
                InstanceInfo instanceInfo = registry.getInstanceInfo(instanceId);
                for (AppInfo appInfo : instanceInfo.getAppInfos()) {
                    String appName = appInfo.getName();
                    uniqueApps.computeIfAbsent(appName, k -> new HashSet<>()).add(instanceId);
                }
            }
        } catch (Exception ignored) {
        }

        List<Map<String, String>> finalAppList = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : uniqueApps.entrySet()) {
            String appName = entry.getKey();
            Set<String> instances = entry.getValue();
            Map<String, String> appMap = new HashMap<>();
            appMap.put("id", appName);
            appMap.put("instances", String.join(",", instances));
            appMap.put("filename", !(DeployerConstants.INSTANCE_APP_NAME.equals(appName) || DeployerConstants.MASTER_APP_NAME.equals(appName)) ? "apps/" + appName : "");
            finalAppList.add(appMap);
        }

        int totalSize = finalAppList.size();
        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalSize);

        List<Map<String, String>> pagedApps = finalAppList.subList(startIndex, endIndex);
        for (Map<String, String> app : pagedApps) {
            response.addData(app);
        }

        response.setTotalSize(totalSize);
        response.setPageSize(pageSize);
        response.setPageNum(pageNum);
    }

    @ModelAction(
            name = {"部署", "en:Deploy"},
            info = {"部署应用到本地实例。", "en:Deploy the app to an on-premises instance."})
    public void create(Request request, Response response) throws Exception {
        response.addModelData(new App());
    }

    @ModelAction(
            name = {"安装", "en:Install"},
            info = {"按配置要求安装应用到指定的实例。", "en:Install the app to the specified instance as required."})
    public void add(Request request, Response response) throws Exception {
        String[] instances = new String[]{DeployerConstants.MASTER_APP_DEFAULT_INSTANCE_ID}/*request.getParameter("instances") != null
                ? request.getParameter("instances").split(",")
                : new String[0]*/;
        ((RequestImpl) request).setModelName("appinstaller");
        ((RequestImpl) request).setActionName("installApp");
        try {
            for (String instance : instances) {
                try {
                    if (DeployerConstants.MASTER_APP_DEFAULT_INSTANCE_ID.equals(instance)) { // 安装到本地节点
                        MasterApp.getService(Deployer.class).getApp(DeployerConstants.INSTANCE_APP_NAME).invokeDirectly(request, response);
                    } else {
                        // TODO：调用远端 instance 上的app add
                    }
                } catch (Exception e) { // todo 部分失败，如何显示到页面？
                    response.setSuccess(false);
                    response.setMsg(e.getMessage());
                    return;
                }
            }
        } finally {
            ((RequestImpl) request).setModelName(DeployerConstants.MASTER_APP_APP_MODEL_NAME);
            ((RequestImpl) request).setActionName(Createable.ACTION_NAME_ADD);
        }
    }

    @ModelAction(
            show = "id!=master&id!=instance",
            name = {"管理", "en:Manage"}, order = 1,
            info = {"转到此应用的管理页面。", "en:Go to the administration page for this app."})
    public void manage(Request request, Response response) throws Exception {
    }

    @ModelAction(
            batch = true, order = 2, show = "id!=master&id!=instance",
            name = {"卸载", "en:Uninstall"},
            info = {"卸载应用，只能卸载本地实例部署的应用。注：请谨慎操作，删除后不可恢复。",
                    "en:If you uninstall an application, you can only uninstall an application deployed on a local instance. Note: Please operate with caution, it cannot be recovered after deletion."})
    public void delete(Request request, Response response) throws Exception {
        String appName = request.getId();
        Deployer deployer = MasterApp.getService(Deployer.class);
        qingzhou.deployer.App app = deployer.getApp(appName);

        ((RequestImpl) request).setAppName(DeployerConstants.INSTANCE_APP_NAME);
        ((RequestImpl) request).setModelName("appinstaller");
        ((RequestImpl) request).setActionName("unInstallApp");
        try {
            if (app != null) {
                deployer.getApp(DeployerConstants.INSTANCE_APP_NAME).invokeDirectly(request, response);
            } else {
                response.setSuccess(false);
                response.setMsg(appContext.getI18n(request.getLang(), "app.delete.notlocal"));
                return;
            }

            // 卸载远程实例
            Registry registry = MasterApp.getService(Registry.class);
            AppInfo appInfo = registry.getAppInfo(appName);
            if (appInfo != null) {
                for (String instanceId : registry.getAllInstanceId()) {
                    InstanceInfo instanceInfo = registry.getInstanceInfo(instanceId);
                    for (AppInfo info : instanceInfo.getAppInfos()) {
                        if (appName.equals(info.getName())) {
                            ((RequestImpl) request).setAppName(instanceId);
//                             deployer.getApp(qingzhou.deployer.App.INSTANCE_APP).invokeDirectly(request, response);// todo 需要远程请求
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (e.getMessage().contains("App not found:")) { // todo 部分失败，如何显示到页面？
                return;
            }
            response.setSuccess(false);
            response.setMsg(e.getMessage());
        } finally {
            ((RequestImpl) request).setManageType(DeployerConstants.MANAGE_TYPE_APP);
            ((RequestImpl) request).setAppName(DeployerConstants.MASTER_APP_NAME);
            ((RequestImpl) request).setModelName(DeployerConstants.MASTER_APP_APP_MODEL_NAME);
            ((RequestImpl) request).setActionName(Deletable.ACTION_NAME_DELETE);
        }
    }
}
