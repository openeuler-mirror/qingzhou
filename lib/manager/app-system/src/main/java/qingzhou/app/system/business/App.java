package qingzhou.app.system.business;

import qingzhou.api.*;
import qingzhou.api.type.Add;
import qingzhou.api.type.Delete;
import qingzhou.app.system.Main;
import qingzhou.app.system.ModelUtil;
import qingzhou.core.deployer.Deployer;
import qingzhou.core.DeployerConstants;
import qingzhou.core.deployer.RequestImpl;
import qingzhou.core.registry.AppInfo;
import qingzhou.core.registry.Registry;

import java.util.*;

@Model(code = DeployerConstants.MODEL_APP, icon = "cube-alt",
        menu = Main.Business, order = 1,
        name = {"应用", "en:App"},
        info = {"应用，是一种按照“轻舟应用开发规范”编写的软件包，可安装在轻舟平台上，用于管理特定的业务系统。",
                "en:Application is a software package written in accordance with the \"Qingzhou Application Development Specification\", which can be deployed on the Qingzhou platform and used to manage specific business systems."})
public class App extends ModelBase implements qingzhou.api.type.List, Add {
    public static final String INSTANCE_SP = ";";

    @Override
    public String idField() {
        return "name";
    }

    @Override
    public String[] allIds(Map<String, String> query) {
        Set<String> allAppNames = new HashSet<>();

        Deployer deployer = Main.getService(Deployer.class);
        deployer.getAllApp().forEach(a -> {
            if (DeployerConstants.APP_SYSTEM.equals(a)) return;
            allAppNames.add(a);
        });

        Registry registry = Main.getService(Registry.class);
        allAppNames.addAll(registry.getAllAppNames());

        List<String> result = new ArrayList<>(allAppNames);

        result.removeIf(id -> !ModelUtil.query(query, new ModelUtil.Supplier() {
            @Override
            public String getModelName() {
                return DeployerConstants.MODEL_APP;
            }

            @Override
            public Map<String, String> get() {
                return showData(id);
            }
        }));

        return result.toArray(new String[0]);
    }

    @ModelField(
            create = false, edit = false,
            forbid = {DeployerConstants.APP_SYSTEM},
            search = true,
            name = {"应用名称", "en:App Name"},
            info = {"应用包的名称，表示该应用的业务系统种类，一种业务系统可安装在多个轻舟实例上，每一次的安装都会有唯一的 ID 与之对应。",
                    "en:The name of the application package indicates the type of business system of the application, and a business system can be deployed on multiple Qingzhou instances, and each deployment will have a unique ID corresponding to it."})
    public String name;

    @ModelField(
            input_type = InputType.bool,
            name = {"使用上传", "en:Enable Upload"},
            info = {"安装的应用可以从客户端上传，也可以从服务器端指定的位置读取。",
                    "en:The installed app can be uploaded from the client or read from a location specified on the server side."})
    public Boolean upload = false;

    @ModelField(
            display = "upload=false",
            required = true,
            file = true,
            name = {"应用位置", "en:Application File"},
            info = {"服务器上应用程序的位置，通常是应用的程序包，注：须为 *.jar, *.zip 类型的文件或目录。",
                    "en:The location of the application on the server, usually the app package, Note: Must be a *.jar, *.zip file or directory."})
    public String path;

    @ModelField(
            display = "upload=true",
            input_type = InputType.file,
            required = true,
            name = {"上传应用", "en:Upload Application"},
            info = {"上传一个应用文件到服务器，文件须是 *.jar 或 *.zip 类型的 Qingzhou 应用文件，否则可能会导致安装失败。",
                    "en:Upload an application file to the server, the file must be a *.jar type qingzhou application file, otherwise the installation may fail."})
    public String file;

    @ModelField(
            input_type = InputType.checkbox,
            required = true,
            reference = Instance.class,
            separator = App.INSTANCE_SP,
            list = true, search = true,
            name = {"安装实例", "en:Instance"},
            info = {"选择安装应用的实例。", "en:Select the instance where you want to install the application."})
    public String instances = DeployerConstants.INSTANCE_LOCAL;

    @ModelField(
            list = true, search = true,
            create = false, edit = false,
            color = {DeployerConstants.APP_STARTED + ":Green", DeployerConstants.APP_STOPPED + ":Gray"},
            name = {"状态", "en:State"},
            info = {"指示应用的当前运行状态。", "en:Indicates the current running state of the app."})
    public String state;

    @Override
    public void start() {
        getAppContext().addI18n("app.id.not.exist", new String[]{"应用文件不存在", "en:The app file does not exist"});
    }

    public Map<String, String> showData(String id) {
        AppInfo appInfo = null;
        List<String> instances = new ArrayList<>();

        qingzhou.core.deployer.App app = Main.getService(Deployer.class).getApp(id);
        if (app != null) {
            appInfo = app.getAppInfo();
            instances = new ArrayList<>();
            instances.add(DeployerConstants.INSTANCE_LOCAL);
        }

        Registry registry = Main.getService(Registry.class);
        if (appInfo == null) {
            appInfo = registry.getAppInfo(id);
        }
        instances.addAll(registry.getAppInstanceNames(id));

        if (appInfo != null) {
            Map<String, String> appMap = new HashMap<>();
            appMap.put(idField(), id);
            appMap.put("path", appInfo.getFilePath());
            appMap.put("instances", String.join(App.INSTANCE_SP, instances));
            appMap.put("state", appInfo.getState());
            return appMap;
        }

        return null;
    }

    @Override
    public List<String[]> listData(int pageNum, int pageSize, String[] showFields, Map<String, String> query) throws Exception {
        return ModelUtil.listData(allIds(query), this::showData, pageNum, pageSize, showFields);
    }

    @ModelAction(
            code = DeployerConstants.ACTION_MANAGE, icon = "location-arrow",
            show = "state=" + DeployerConstants.APP_STARTED,
            name = {"管理", "en:Manage"},
            info = {"转到此应用的管理页面。", "en:Go to the administration page for this app."})
    public void manage(Request request) {
    }

    @Override
    public String[] listActions() {
        return new String[]{DeployerConstants.ACTION_MANAGE, "start", "stop", Delete.ACTION_DELETE};
    }

    @ModelAction(
            code = Add.ACTION_CREATE, icon = "plus-sign",
            name = {"安装", "en:Install"},
            info = {"安装应用包到指定的轻舟实例上。",
                    "en:Install the application package to the specified Qingzhou instance."})
    public void create(Request request) throws Exception {
        getAppContext().invokeSuperAction(request);
    }

    @ModelAction(
            code = Add.ACTION_ADD, icon = "save",
            name = {"安装", "en:Install"},
            info = {"安装应用包到指定的轻舟实例上。",
                    "en:Install the application package to the specified Qingzhou instance."})
    public void add(Request request) {
        String instances = ((RequestImpl) request).getParameters().remove("instances");
        Main.invokeAgentOnInstances(request, DeployerConstants.ACTION_INSTALL_APP, instances.split(App.INSTANCE_SP));
    }

    @Override
    public void addData(Map<String, String> data) {
        // 覆写了 Add.ACTION_ADD ，不会再进入这里了
    }

    @ModelAction(
            code = Delete.ACTION_DELETE, icon = "trash",
            action_type = ActionType.action_list,
            name = {"卸载", "en:UnInstall"},
            info = {"卸载应用，注：卸载应用会删除应用包下的所有文件，且不可恢复。",
                    "en:Uninstall the app, Note: Uninstalling the app will delete all the files under the app package and cannot be recovered."})
    public void delete(Request request) {
        String id = request.getId();
        Map<String, String> app = showData(id);
        String instances = app.get("instances");
        Main.invokeAgentOnInstances(request, DeployerConstants.ACTION_UNINSTALL_APP, instances.split(App.INSTANCE_SP));
    }

    @ModelAction(
            code = DeployerConstants.ACTION_START, icon = "play",
            show = "state=" + DeployerConstants.APP_STOPPED,
            action_type = ActionType.action_list,
            name = {"启动", "en:start"},
            info = {"启动应用", "en:Launch the application."})
    public void startApp(Request request) {
        String id = request.getId();
        Map<String, String> app = showData(id);
        String instances = app.get("instances");
        Main.invokeAgentOnInstances(request, DeployerConstants.ACTION_START_APP, instances.split(App.INSTANCE_SP));
    }

    @ModelAction(
            code = DeployerConstants.ACTION_STOP, icon = "stop",
            show = "state=" + DeployerConstants.APP_STARTED,
            action_type = ActionType.action_list,
            name = {"停止", "en:end"},
            info = {"停止应用", "en:stop the application."})
    public void stopApp(Request request) {
        String id = request.getId();
        Map<String, String> app = showData(id);
        String instances = app.get("instances");
        Main.invokeAgentOnInstances(request, DeployerConstants.ACTION_STOP_APP, instances.split(App.INSTANCE_SP));
    }
}