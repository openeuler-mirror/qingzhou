package qingzhou.app.system.service;

import qingzhou.api.*;
import qingzhou.api.type.Listable;
import qingzhou.app.system.Main;
import qingzhou.deployer.ActionInvoker;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.RequestImpl;
import qingzhou.registry.AppInfo;
import qingzhou.registry.InstanceInfo;
import qingzhou.registry.Registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Model(code = DeployerConstants.MODEL_APP, icon = "cube-alt",
        menu = "Service", order = 1,
        name = {"应用", "en:App"},
        info = {"应用，是一种按照“轻舟应用开发规范”编写的软件包，可部署在轻舟平台上，用于管理特定的业务系统。",
                "en:Application is a software package written in accordance with the \"Qingzhou Application Development Specification\", which can be deployed on the Qingzhou platform and used to manage specific business systems."})
public class App extends ModelBase implements Listable {
    private final String SP = DeployerConstants.DEFAULT_DATA_SEPARATOR;

    @ModelField(
            editable = false,
            unsupportedStrings = {DeployerConstants.APP_SYSTEM},
            list = true,
            name = {"名称", "en:Name"},
            info = {"应用的名称信息，用以识别业务系统。",
                    "en:The name of the application to identify the business system."})
    public String id;

    @ModelField(
            type = FieldType.bool,
            name = {"使用上传", "en:Enable Upload"},
            info = {"安装的应用可以从客户端上传，也可以从服务器端指定的位置读取。",
                    "en:The installed app can be uploaded from the client or read from a location specified on the server side."})
    public boolean upload = false;

    @ModelField(
            show = "upload=false",
            required = true,
            filePath = true,
            list = true,
            name = {"应用位置", "en:Application File"},
            info = {"服务器上应用程序的位置，通常是应用的程序包，注：须为 *.jar, *.zip 类型的文件或目录。",
                    "en:The location of the application on the server, usually the app package, Note: Must be a *.jar, *.zip file or directory."})
    public String path;

    @ModelField(
            show = "upload=true",
            type = FieldType.file,
            required = true,
            name = {"上传应用", "en:Upload Application"},
            info = {"上传一个应用文件到服务器，文件须是 *.jar 或 *.zip 类型的 QingZhou 应用文件，否则可能会导致安装失败。",
                    "en:Upload an application file to the server, the file must be a *.jar type qingzhou application file, otherwise the installation may fail."})
    public String file;

    @ModelField(
            type = FieldType.multiselect,
            required = true,
            options = {DeployerConstants.INSTANCE_LOCAL},
            list = true, //refModel = Instance.class, todo 远程获取引用model的列表
            name = {"实例", "en:Instance"},
            info = {"选择安装应用的实例。", "en:Select the instance where you want to install the application."})
    public String instances = DeployerConstants.INSTANCE_LOCAL;

    @Override
    public void start() {
        appContext.addI18n("app.id.not.exist", new String[]{"应用文件不存在", "en:The app file does not exist"});
        appContext.addI18n("app.type.unknown", new String[]{"未知的应用类型", "en:Unknown app type"});
    }

    @Override
    public Map<String, String> showData(String id) {
        qingzhou.deployer.App app = Main.getService(Deployer.class).getApp(id);
        if (app != null) {
            Map<String, String> appMap = new HashMap<>();
            appMap.put(idFieldName(), id);
            appMap.put("instances", DeployerConstants.INSTANCE_LOCAL);
            appMap.put("path", app.getAppContext().getAppDir().getAbsolutePath());
            return appMap;
        }

        Registry registry = Main.getService(Registry.class);
        Map<String, String> appMap = new HashMap<>();
        for (String instance : registry.getAllInstanceId()) {
            InstanceInfo instanceInfo = registry.getInstanceInfo(instance);
            for (AppInfo appInfo : instanceInfo.getAppInfos()) {
                if (appInfo.getName().equals(id)) {
                    if (!appMap.containsKey(idFieldName())) {
                        appMap.put(idFieldName(), id);
                        appMap.put("instances", instanceInfo.getId());
                        appMap.put("path", appInfo.getFilePath());
                    } else {
                        appMap.put("instances", appMap.get("instances")
                                + SP + instanceInfo.getId());
                    }
                }
            }
        }

        return null;
    }

    @Override
    public List<Map<String, String>> listData(int pageNum, int pageSize, String[] fieldNames) {
        List<String> allAppNames = listAllAppNames();
        int totalSize = allAppNames.size();
        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalSize);
        List<String> subList = allAppNames.subList(startIndex, endIndex);
        List<Map<String, String>> data = new ArrayList<>();
        subList.forEach(a -> data.add(showData(a)));
        return data;
    }

    private List<String> listAllAppNames() {
        List<String> allAppNames = new ArrayList<>();

        Main.getService(Deployer.class).getAllApp().forEach(a -> {
            if (DeployerConstants.APP_SYSTEM.equals(a)) return;
            allAppNames.add(a);
        });

        Registry registry = Main.getService(Registry.class);
        allAppNames.addAll(registry.getAllAppNames());

        return allAppNames;
    }

    @ModelAction(
            code = DeployerConstants.ACTION_DELETE, icon = "trash", order = 9,
            ajax = true,
            batch = true,
            name = {"卸载", "en:Uninstall"},
            info = {"卸载应用，只能卸载本地实例部署的应用。注：请谨慎操作，删除后不可恢复。",
                    "en:If you uninstall an application, you can only uninstall an application deployed on a local instance. Note: Please operate with caution, it cannot be recovered after deletion."})
    public void delete(Request request) throws Exception {
        String appName = request.getId();
        Map<String, String> app = showData(appName);
        String[] instances = app.get("instances").split(SP);

        RequestImpl tmpReq = new RequestImpl();
        tmpReq.setId(appName);
        tmpReq.setAppName(DeployerConstants.APP_SYSTEM);
        tmpReq.setModelName(DeployerConstants.MODEL_INSTALLER);
        tmpReq.setActionName(DeployerConstants.ACTION_UNINSTALL);

        List<Response> responseList = Main.getService(ActionInvoker.class).invokeOnInstances(tmpReq, instances);
        request.getResponse().setSuccess(responseList.isEmpty());

        if (!responseList.isEmpty()) {
            // todo 参考 ActionInvoker 的 invokeBatch 方法，给出友好的响应信息
        }
    }

    @ModelAction(
            code = DeployerConstants.ACTION_UPDATE, icon = "save",
            ajax = true,
            name = {"更新", "en:Update"},
            info = {"更新这个模块的配置信息。", "en:Update the configuration information for this module."})
    public void update(Request request) throws Exception {
        delete(request);
        add(request);
    }

    @ModelAction(
            code = DeployerConstants.ACTION_ADD, icon = "save",
            ajax = true,
            name = {"添加", "en:Add"},
            info = {"按配置要求创建一个模块。", "en:Create a module as configured."})
    public void add(Request request) throws Exception {
        RequestImpl tmpReq = new RequestImpl();
        tmpReq.setAppName(DeployerConstants.APP_SYSTEM);
        tmpReq.setModelName(DeployerConstants.MODEL_INSTALLER);
        tmpReq.setActionName(DeployerConstants.ACTION_INSTALL);
        tmpReq.setParameters(request.getParameters());

        String[] instances = request.getParameter("instances").split(SP);
        List<Response> responseList = Main.getService(ActionInvoker.class).invokeOnInstances(tmpReq, instances);
        request.getResponse().setSuccess(responseList.isEmpty());

        if (!responseList.isEmpty()) {
            // todo 参考 ActionInvoker 的 invokeBatch 方法，给出友好的响应信息
        }
    }

    @ModelAction(
            code = DeployerConstants.ACTION_MANAGE, icon = "location-arrow",
            order = 1,
            name = {"管理", "en:Manage"},
            info = {"转到此应用的管理页面。", "en:Go to the administration page for this app."})
    public void manage(Request request) {
    }
}
