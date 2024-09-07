package qingzhou.app.system.service;

import qingzhou.api.*;
import qingzhou.api.type.Addable;
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
        menu = Main.SERVICE_MENU, order = 1,
        name = {"应用", "en:App"},
        info = {"应用，是一种按照“轻舟应用开发规范”编写的软件包，可部署在轻舟平台上，用于管理特定的业务系统。",
                "en:Application is a software package written in accordance with the \"Qingzhou Application Development Specification\", which can be deployed on the Qingzhou platform and used to manage specific business systems."})
public class App extends ModelBase implements Addable {
    private final String SP = DeployerConstants.DEFAULT_DATA_SEPARATOR;

    @ModelField(
            required = true,
            editable = false,
            unsupportedStrings = {DeployerConstants.APP_SYSTEM},
            list = true,
            name = {"应用ID", "en:App ID"},
            info = {"应用的名称，用以区别业务系统。",
                    "en:The name of the application to distinguish the business system."})
    public String id;

    @ModelField(
            required = true,
            createable = false, editable = false,
            unsupportedStrings = {DeployerConstants.APP_SYSTEM},
            list = true,
            name = {"应用类型", "en:App Type"},
            info = {"应用的类型信息，表示该应用的业务系统种类，一种业务系统可部署在多个轻舟实例上，此时它们的应用类型相同，但应用ID不同。",
                    "en:The type of application indicates the type of business system of the application, and a business system can be deployed on multiple Qingzhou instances, and their application types are the same, but the application IDs are different."})
    public String type;

    @ModelField(
            type = FieldType.bool,
            name = {"使用上传", "en:Enable Upload"},
            info = {"安装的应用可以从客户端上传，也可以从服务器端指定的位置读取。",
                    "en:The installed app can be uploaded from the client or read from a location specified on the server side."})
    public Boolean upload = false;

    @ModelField(
            show = "upload=false",
            required = true,
            filePath = true,
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
            code = DeployerConstants.ACTION_MANAGE, icon = "location-arrow",
            order = 1,
            name = {"管理", "en:Manage"},
            info = {"转到此应用的管理页面。", "en:Go to the administration page for this app."})
    public void manage(Request request) {
    }

    @Override
    public void addData(Map<String, String> data) throws Exception {
        RequestImpl tmpReq = new RequestImpl();
        tmpReq.setAppName(DeployerConstants.APP_SYSTEM);
        tmpReq.setModelName(DeployerConstants.MODEL_INSTALLER);
        tmpReq.setActionName(DeployerConstants.ACTION_INSTALL);

        String instances = data.remove("instances");
        tmpReq.setParameters(data);

        List<Response> responseList = Main.getService(ActionInvoker.class).invokeOnInstances(tmpReq, instances.split(SP));
        if (!responseList.isEmpty()) {
            // todo 参考 ActionInvoker 的 invokeBatch 方法，给出友好的响应信息
        }
    }

    @Override
    public void deleteData(String id) throws Exception {
        Map<String, String> app = showData(id);
        String[] instances = app.get("instances").split(SP);

        RequestImpl tmpReq = new RequestImpl();
        tmpReq.setId(id);
        tmpReq.setAppName(DeployerConstants.APP_SYSTEM);
        tmpReq.setModelName(DeployerConstants.MODEL_INSTALLER);
        tmpReq.setActionName(DeployerConstants.ACTION_UNINSTALL);

        List<Response> responseList = Main.getService(ActionInvoker.class).invokeOnInstances(tmpReq, instances);
        if (!responseList.isEmpty()) {
            // todo 参考 ActionInvoker 的 invokeBatch 方法，给出友好的响应信息
        }
    }

    @Override
    public void updateData(Map<String, String> data) throws Exception {
        deleteData(data.get(idFieldName()));
        addData(data);
    }
}
