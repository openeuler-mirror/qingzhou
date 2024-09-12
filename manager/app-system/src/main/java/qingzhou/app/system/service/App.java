package qingzhou.app.system.service;

import qingzhou.api.*;
import qingzhou.api.type.Addable;
import qingzhou.api.type.Deletable;
import qingzhou.api.type.Listable;
import qingzhou.app.system.Main;
import qingzhou.app.system.ModelUtil;
import qingzhou.deployer.ActionInvoker;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.RequestImpl;
import qingzhou.registry.AppInfo;
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
public class App extends ModelBase implements Listable {
    @Override
    public String idFieldName() {
        return "name";
    }

    @Override
    public String[] allIds() {
        List<String> allAppNames = new ArrayList<>();

        Main.getService(Deployer.class).getAllApp().forEach(a -> {
            if (DeployerConstants.APP_SYSTEM.equals(a)) return;
            allAppNames.add(a);
        });

        Registry registry = Main.getService(Registry.class);
        allAppNames.addAll(registry.getAllAppNames());

        return allAppNames.toArray(new String[0]);
    }

    @ModelField(
            required = true,
            createable = false, editable = false,
            unsupportedStrings = {DeployerConstants.APP_SYSTEM},
            list = true,
            name = {"应用名称", "en:App Name"},
            info = {"应用包的名称，表示该应用的业务系统种类，一种业务系统可部署在多个轻舟实例上，每一次的部署都会有唯一的 ID 与之对应。",
                    "en:The name of the application package indicates the type of business system of the application, and a business system can be deployed on multiple Qingzhou instances, and each deployment will have a unique ID corresponding to it."})
    public String name;

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
            type = FieldType.checkbox,
            required = true,
            refModel = Instance.class,
            list = true,
            name = {"部署实例", "en:Instance"},
            info = {"选择安装应用的实例。", "en:Select the instance where you want to install the application."})
    public String instances = DeployerConstants.INSTANCE_LOCAL;

    @Override
    public void start() {
        appContext.addI18n("app.id.not.exist", new String[]{"应用文件不存在", "en:The app file does not exist"});
    }

    @Override
    public Map<String, String> showData(String id) {
        AppInfo appInfo;
        List<String> instances;

        qingzhou.deployer.App app = Main.getService(Deployer.class).getApp(id);
        if (app != null) {
            appInfo = app.getAppInfo();
            instances = new ArrayList<>();
            instances.add(DeployerConstants.INSTANCE_LOCAL);
        } else {
            Registry registry = Main.getService(Registry.class);
            appInfo = registry.getAppInfo(id);
            instances = registry.getAppInstanceNames(id);
        }

        if (appInfo != null) {
            Map<String, String> appMap = new HashMap<>();
            appMap.put(idFieldName(), id);
            appMap.put("path", appInfo.getFilePath());
            appMap.put("instances", String.join(DeployerConstants.DEFAULT_DATA_SEPARATOR, instances));
            return appMap;
        }

        return null;
    }

    @Override
    public List<Map<String, String>> listData(int pageNum, int pageSize, String[] fieldNames) {
        return ModelUtil.listData(allIds(), this::showData, pageNum, pageSize, fieldNames);
    }

    @Override
    public int totalSize() {
        return allIds().length;
    }

    @ModelAction(
            code = DeployerConstants.ACTION_MANAGE, icon = "location-arrow",
            order = 1,
            name = {"管理", "en:Manage"},
            info = {"转到此应用的管理页面。", "en:Go to the administration page for this app."})
    public void manage(Request request) {
    }

    @ModelAction(
            code = Addable.ACTION_CREATE, icon = "plus-sign",
            name = {"部署", "en:Deploy"},
            info = {"部署应用包到指定的轻舟实例上。",
                    "en:Deploy the application package to the specified Qingzhou instance."})
    public void create(Request request) throws Exception {
        request.getResponse().addModelData(new App());
    }

    @ModelAction(
            code = Addable.ACTION_ADD, icon = "save",
            name = {"部署", "en:Deploy"},
            info = {"部署应用包到指定的轻舟实例上。",
                    "en:Deploy the application package to the specified Qingzhou instance."})
    public void add(Request request) {
        RequestImpl tmpReq = buildAgentRequest(appContext.getRequestLang());
        tmpReq.setActionName(DeployerConstants.ACTION_INSTALL);

        String app = Boolean.parseBoolean(request.getParameter("upload"))
                ? request.getParameter("file")
                : request.getParameter("path");
        tmpReq.setParameter(DeployerConstants.INSTALLER_PARAMETER_FILE_ID, app);

        String instance = ((RequestImpl) request).removeParameter("instances");

        invokeOnInstances(tmpReq, instance, request);
    }

    @ModelAction(
            code = Deletable.ACTION_DELETE, icon = "trash",
            order = 9,
            batch = true,
            name = {"删除", "en:Delete"},
            info = {"删除本条数据，注：请谨慎操作，删除后不可恢复。",
                    "en:Delete this data, note: Please operate with caution, it cannot be restored after deletion."})
    public void delete(Request request) {
        String id = request.getId();
        Map<String, String> app = showData(id);

        RequestImpl tmpReq = buildAgentRequest(appContext.getRequestLang());
        tmpReq.setId(id);
        tmpReq.setActionName(DeployerConstants.ACTION_UNINSTALL);

        invokeOnInstances(tmpReq, app.get("instances"), request);
    }

    private void invokeOnInstances(Request tmpReq, String instance, Request request) {
        List<Response> responseList = Main.getService(ActionInvoker.class)
                .invokeOnInstances(tmpReq, instance.split(DeployerConstants.DEFAULT_DATA_SEPARATOR));
        final StringBuilder[] error = {null};
        responseList.forEach(response -> {
            if (!response.isSuccess()) {
                request.getResponse().setSuccess(false);
                if (error[0] == null) {
                    error[0] = new StringBuilder();
                }
                error[0].append(response.getMsg());
            }
        });

        if (!request.getResponse().isSuccess()) {
            String errorMsg = error[0].toString();
            request.getResponse().setMsg(errorMsg);
        }
    }

    private RequestImpl buildAgentRequest(Lang lang) {
        RequestImpl tmpReq = new RequestImpl();
        tmpReq.setAppName(DeployerConstants.APP_SYSTEM);
        tmpReq.setModelName(DeployerConstants.MODEL_AGENT);
        tmpReq.setI18nLang(lang);
        return tmpReq;
    }
}
