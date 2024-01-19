package qingzhou.app.master.service;

import qingzhou.app.master.Main;
import qingzhou.framework.AppManager;
import qingzhou.framework.api.AddModel;
import qingzhou.framework.api.ConsoleContext;
import qingzhou.framework.api.FieldType;
import qingzhou.framework.api.ListModel;
import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelAction;
import qingzhou.framework.api.ModelBase;
import qingzhou.framework.api.ModelField;
import qingzhou.framework.api.Option;
import qingzhou.framework.api.Options;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;
import qingzhou.framework.console.ConsoleConstants;
import qingzhou.framework.console.ConsoleContextCache;
import qingzhou.framework.util.ExceptionUtil;
import qingzhou.framework.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Model(name = "app", icon = "cube-alt",
        menuName = "Service", menuOrder = 1,
        nameI18n = {"应用", "en:App"},
        infoI18n = {"应用。",
                "en:App Management."})
public class App extends ModelBase implements AddModel {
    @ModelField(
            showToList = true,
            disableOnCreate = true, disableOnEdit = true,
            nameI18n = {"名称", "en:Name"},
            infoI18n = {"应用名称。", "en:App Name"})
    public String id;

    @ModelField(
            required = true,
            disableOnEdit = true,
            type = FieldType.bool,
            nameI18n = {"使用上传", "en:Enable Upload"},
            infoI18n = {"部署的应用可以从客户端上传，也可以从服务器端指定的位置读取。注：出于安全考虑，QingZhou 出厂设置禁用了文件上传功能，您可在“控制台安全”模块了解详情和进行相关的配置操作。",
                    "en:The deployed app can be uploaded from the client or read from a location specified on the server side. Note: For security reasons, QingZhou factory settings disable file upload, you can learn more and perform related configuration operations in the \"Console Security\" module."})
    public boolean appFrom = false;

    @ModelField(
            showToList = true,
            effectiveWhen = "appFrom=false",
            disableOnEdit = true,
            required = true,
            notSupportedCharacters = "#",
            maxLength = 255,// for #NC-1418 及其它文件目录操作的，文件长度不能大于 255
            nameI18n = {"应用位置", "en:Application File"},
            infoI18n = {"服务器上应用程序的位置，通常是应用的程序包，注：须为 *.jar 类型的文件。",
                    "en:The location of the application on the server, usually the app package, Note: Must be a *.jar file."})
    public String filename;

    @ModelField(
            type = FieldType.file,
            effectiveWhen = "appFrom=true",
            disableOnEdit = true,
            showToEdit = false,
            notSupportedCharacters = "#",
            required = true,
            nameI18n = {"上传应用", "en:Upload Application"},
            infoI18n = {"上传一个应用文件到服务器，文件须是 *.jar 类型的轻舟应用文件，否则可能会导致部署失败。",
                    "en:Upload an application file to the server, the file must be a *.jar type qingzhou application file, otherwise the deployment may fail."})
    public String fromUpload;

    @ModelField(
            required = true, type = FieldType.checkbox,
            refModel = "node", showToList = true,
            nameI18n = {"节点", "en:Node"},
            infoI18n = {"选择部署应用的节点。", "en:Select the node where you want to deploy the application."})
    public String nodes;

    @ModelField(
            showToList = true,
            nameI18n = {"应用版本", "en:Instance Version"},
            infoI18n = {"此应用的版本。", "en:The version of this app."})
    public String version;

    @ModelField(
            showToList = true,
            nameI18n = {"类型", "en:Type"},
            infoI18n = {"此应用的类型。", "en:The type of this app."})
    public String type;

    @Override
    public void init() {
        super.init();
        getAppContext().getConsoleContext().addI18N("app.id.system", new String[]{"该名称已被系统占用，请更换为其它名称", "en:This name is already occupied by the system, please replace it with another name"});
    }

    @Override
    public String validate(Request request, String fieldName) {
        if (fieldName.equals(ListModel.FIELD_NAME_ID)) {
            String id = request.getParameter(ListModel.FIELD_NAME_ID);
            if (ConsoleConstants.MASTER_APP_NAME.equals(id) ||
                    ConsoleConstants.NODE_APP_NAME.equals(id)) {
                return getAppContext().getConsoleContext().getI18N("app.id.system");
            }
        }

        return super.validate(request, fieldName);
    }

    @Override
    public Options options(String fieldName) {
        if ("nodes".equals(fieldName)) {
            Options options = super.options(fieldName);
            return Options.merge(options, Option.of(ConsoleConstants.LOCAL_NODE_NAME, new String[]{"默认节点", "en:default"}));
        }

        return super.options(fieldName);
    }

    @Override
    @ModelAction(name = ACTION_NAME_CREATE,
            showToListHead = true,
            icon = "plus-sign", forwardToPage = "form",
            nameI18n = {"部署", "en:Deploy"},
            infoI18n = {"获得创建该组件的默认数据或界面。", "en:Get the default data or interface for creating this component."})
    public void create(Request request, Response response) throws Exception {
        AddModel.super.create(request, response);
    }

    @Override
    @ModelAction(name = ACTION_NAME_ADD,
            icon = "save",
            showToFormBottom = true,
            nameI18n = {"部署", "en:Deploy"},
            infoI18n = {"按配置要求部署应用到指定的节点。", "en:Deploy the app to the specified node as required."})
    public void add(Request request, Response response) throws Exception {
        Map<String, String> p = prepareParameters(request);
        File srcFile;
        if (Boolean.parseBoolean(p.get("appFrom"))) {
            srcFile = FileUtil.newFile(p.remove("fromUpload"));
        } else {
            srcFile = new File(p.get("filename"));
        }
        if (!srcFile.exists() || !srcFile.isFile()) {
            return;
        }
        String srcFileName = srcFile.getName();
        String appName;
        if (srcFile.isDirectory()) {
            appName = srcFileName;
            File app = FileUtil.newFile(getAppsDir(), appName);
            FileUtil.copyFileOrDirectory(srcFile, app);
        } else if (srcFileName.endsWith(".jar")) {
            int index = srcFileName.lastIndexOf(".");
            appName = srcFileName.substring(0, index);
            File app = FileUtil.newFile(getAppsDir(), appName);
            FileUtil.copyFileOrDirectory(srcFile, FileUtil.newFile(app, "lib", srcFileName));
        } else if (srcFileName.endsWith(".zip")) {
            int index = srcFileName.lastIndexOf(".");
            appName = srcFileName.substring(0, index);
            File app = FileUtil.newFile(getAppsDir(), appName);
            FileUtil.unZipToDir(srcFile, app);// todo: 如果不需要部署到 本地节点，这里的解压就没有必要了
        } else {
            throw ExceptionUtil.unexpectedException("unknown app type");
        }

        String[] nodes = p.get("nodes").split(ConsoleConstants.DATA_SEPARATOR);
        for (String node : nodes) {
            if (ConsoleConstants.LOCAL_NODE_NAME.equals(node)) { // 安装到本地节点
                File app = FileUtil.newFile(getAppsDir(), appName);
                try {
                    getAppManager().installApp(app);
                    p.put("id", appName);
                    getDataStore().addData(request.getModelName(), appName, p);
                } catch (Exception e) {
                    e.printStackTrace();
                    FileUtil.forceDeleteQuietly(app);
                    response.setSuccess(false);
                }
            } else {
                // TODO：调用远端 node 上 master 的 app add？（将其node设置为 LOCAL_NODE_NAME 后在发送远程请求？）
            }
        }
    }

    @Override
    public void delete(Request request, Response response) throws Exception {
        String id = request.getId();
        Map<String, String> appInfo = getDataStore().getDataById("app", id);
        if (appInfo == null || appInfo.isEmpty()) {
            return;
        }

        try {
            if (Main.getFC().getAppManager().getApps().contains(id)) {
                getAppManager().uninstallApp(id);
            } else {
                // TODO 这里还要卸载远程节点的
            }
            ConsoleContextCache.removeAppConsoleContext(id);

            File app = FileUtil.newFile(getAppsDir(), id);
            if (app.exists()) {
                try {
                    FileUtil.forceDelete(app);
                } catch (IOException e) {
                    e.printStackTrace();
                    response.setSuccess(false);
                    return;
                }
            }

            getDataStore().deleteDataById(request.getModelName(), id);
        } catch (Exception e) {
            e.printStackTrace();
            response.setSuccess(false);
        }
    }

    @ModelAction(name = "target",
            icon = "location-arrow", forwardToPage = "target",
            nameI18n = {"管理", "en:Manage"}, showToList = true,
            infoI18n = {"转到此实例的管理页面。", "en:Go to the administration page for this instance."})
    public void switchTarget(Request request, Response response) throws Exception {
        if (ConsoleContextCache.getAppConsoleContext(request.getAppName()) == null) {
            // TODO 需要从远程获取应用的ConsoleContext
            ConsoleContext context = null;
            ConsoleContextCache.addAppConsoleContext(request.getAppName(), context);
        }
    }

    private AppManager getAppManager() {
        return Main.getFC().getAppManager();
    }

    public File getAppsDir() {
        File apps = FileUtil.newFile(Main.getFC().getDomain(), "apps");
        if (!apps.exists()) {
            FileUtil.mkdirs(apps);
        }

        return apps;
    }
}
