package qingzhou.app.master.service;

import qingzhou.app.master.Main;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.RequestImpl;
import qingzhou.framework.api.AddModel;
import qingzhou.framework.api.FieldType;
import qingzhou.framework.api.ListModel;
import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelAction;
import qingzhou.framework.api.ModelBase;
import qingzhou.framework.api.ModelField;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;
import qingzhou.framework.util.FileUtil;

import java.io.File;
import java.util.Map;

@Model(name = FrameworkContext.SYS_MODEL_APP, icon = "cube-alt",
        menuName = "Service", menuOrder = 1,
        nameI18n = {"应用", "en:App"},
        infoI18n = {"应用。",
                "en:App Management."})
public class App extends ModelBase implements AddModel {

    @ModelField(
            showToList = true,
            disableOnCreate = true,
            disableOnEdit = true,
            nameI18n = {"名称", "en:Name"},
            infoI18n = {"应用名称。", "en:App Name"})
    public String id;

    @ModelField(
            required = true,
            disableOnEdit = true,
            showToEdit = false,
            type = FieldType.bool,
            nameI18n = {"使用上传", "en:Enable Upload"},
            infoI18n = {"安装的应用可以从客户端上传，也可以从服务器端指定的位置读取。注：出于安全考虑，Qingzhou 出厂设置禁用了文件上传功能，您可在“控制台安全”模块了解详情和进行相关的配置操作。",
                    "en:The installed app can be uploaded from the client or read from a location specified on the server side. Note: For security reasons, Qingzhou factory settings disable file upload, you can learn more and perform related configuration operations in the \"Console Security\" module."})
    public boolean appFrom = false;

    @ModelField(
            showToList = true,
            effectiveWhen = "appFrom=false",
            disableOnEdit = true,
            showToEdit = false,
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
            infoI18n = {"上传一个应用文件到服务器，文件须是 *.jar 或 *.zip 类型的轻舟应用文件，否则可能会导致安装失败。",
                    "en:Upload an application file to the server, the file must be a *.jar type qingzhou application file, otherwise the installation may fail."})
    public String fromUpload;

    @ModelField(
            required = true, type = FieldType.checkbox,
            refModel = "node", showToList = true,
            nameI18n = {"节点", "en:Node"},
            infoI18n = {"选择安装应用的节点。", "en:Select the node where you want to install the application."})
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
        getAppContext().getConsoleContext().addI18N("app.id.system", new String[]{"该名称已被系统占用，请更换为其它名称", "en:This name is already occupied by the system, please replace it with another name"});
        getAppContext().getConsoleContext().addI18N("app.id.not.exist", new String[]{"应用文件不存在", "en:The app file does not exist"});
        getAppContext().getConsoleContext().addI18N("app.type.unknown", new String[]{"未知的应用类型", "en:Unknown app type"});
    }

    @Override
    public String validate(Request request, String fieldName) {
        if (fieldName.equals(ListModel.FIELD_NAME_ID)) {
            String id = request.getParameter(ListModel.FIELD_NAME_ID);
            if (FrameworkContext.SYS_APP_MASTER.equals(id) ||
                    FrameworkContext.SYS_APP_NODE_AGENT.equals(id)) {
                return "app.id.system";
            }
        }

        return null;
    }

    @Override
    @ModelAction(name = ACTION_NAME_ADD,
            icon = "save",
            showToFormBottom = true,
            nameI18n = {"安装", "en:Install"},
            infoI18n = {"按配置要求安装应用到指定的节点。", "en:Install the app to the specified node as required."})
    public void add(Request req, Response response) throws Exception {
        RequestImpl request = (RequestImpl) req;
        Map<String, String> p = prepareParameters(request);
        File srcFile;
        if (Boolean.parseBoolean(p.remove("appFrom"))) {
            srcFile = FileUtil.newFile(p.remove("fromUpload"));
        } else {
            srcFile = new File(p.remove("filename"));
        }
        if (!srcFile.exists() || !srcFile.isFile()) {
            response.setSuccess(false);
            String msg = getAppContext().getConsoleContext().getI18N(request.getI18nLang(), "app.id.not.exist");
            response.setMsg(msg);
            return;
        }
        String srcFileName = srcFile.getName();
        String appName;
        if (srcFile.isDirectory()) {
            appName = srcFileName;
        } else if (srcFileName.endsWith(".jar") || srcFileName.endsWith(".zip")) {
            int index = srcFileName.lastIndexOf(".");
            appName = srcFileName.substring(0, index);
        } else {
            response.setSuccess(false);
            String msg = getAppContext().getConsoleContext().getI18N(request.getI18nLang(), "app.type.unknown");
            response.setMsg(msg);
            return;
        }

        String[] nodes = p.get("nodes").split(",");
        request.setModelName(FrameworkContext.SYS_MODEL_APP_INSTALLER);
        request.setActionName(FrameworkContext.SYS_ACTION_INSTALL);
        try {
            for (String node : nodes) {
                try {
                    if (FrameworkContext.SYS_NODE_LOCAL.equals(node)) { // 安装到本地节点
                        Main.getFc().getAppManager().getApp(FrameworkContext.SYS_APP_NODE_AGENT).invoke(request, response);
                    } else {
                        // TODO：调用远端 node 上的app add
                    }
                } catch (Exception e) { // todo 部分失败，如何显示到页面？
                    response.setSuccess(false);
                    response.setMsg(e.getMessage());
                    e.printStackTrace();
                }
            }
        } finally {
            request.setModelName(FrameworkContext.SYS_MODEL_APP);
            request.setActionName(ACTION_NAME_ADD);
        }

        if (response.isSuccess()) {
            p.put(ListModel.FIELD_NAME_ID, appName);
            getDataStore().addData(FrameworkContext.SYS_MODEL_APP, appName, p);
        }
    }

    @ModelAction(name = FrameworkContext.SYS_ACTION_MANAGE,
            icon = "location-arrow", forwardToPage = "sys/" + FrameworkContext.SYS_ACTION_MANAGE,
            nameI18n = {"管理", "en:Manage"}, showToList = true, orderOnList = -1,
            infoI18n = {"转到此应用的管理页面。", "en:Go to the administration page for this app."})
    public void switchTarget(Request request, Response response) throws Exception {
    }

    @Override
    public void delete(Request req, Response response) throws Exception {
        RequestImpl request = (RequestImpl) req;
        String appName = request.getId();
        Map<String, String> p = getDataStore().getDataById("app", appName);
        String[] nodes = p.get("nodes").split(",");

        request.setModelName(FrameworkContext.SYS_MODEL_APP_INSTALLER);
        request.setActionName(FrameworkContext.SYS_ACTION_UNINSTALL);
        try {
            for (String node : nodes) {
                try {
                    if (FrameworkContext.SYS_NODE_LOCAL.equals(node)) { // 安装到本地节点
                        Main.getFc().getAppManager().getApp(FrameworkContext.SYS_APP_NODE_AGENT).invoke(request, response);
                    } else {
                        // TODO：调用远端 node 上的app delete
                    }
                } catch (Exception e) { // todo 部分失败，如何显示到页面？
                    response.setSuccess(false);
                    e.printStackTrace();
                }
            }
        } finally {
            request.setModelName(FrameworkContext.SYS_MODEL_APP);
            request.setActionName(ACTION_NAME_DELETE);
        }
        getDataStore().deleteDataById("app", appName);
    }

    @Override
    @ModelAction(name = ACTION_NAME_CREATE,
            showToListHead = true,
            icon = "plus-sign", forwardToPage = "form",
            nameI18n = {"安装", "en:Install"},
            infoI18n = {"在该节点上安装应用。", "en:Install the application on the node."})
    public void create(Request request, Response response) throws Exception {
        AddModel.super.create(request, response);
    }

    @Override
    public String resolveId(Request request) {
        File appFile;
        if (Boolean.parseBoolean(request.getParameter("appFrom"))) {
            appFile = FileUtil.newFile(request.getParameter("fromUpload"));
        } else {
            appFile = new File(request.getParameter("filename"));
        }

        String appFileName = appFile.getName();
        int i = appFileName.lastIndexOf(".");
        if (i == -1) return appFileName;
        return appFileName.substring(0, i);
    }
}
