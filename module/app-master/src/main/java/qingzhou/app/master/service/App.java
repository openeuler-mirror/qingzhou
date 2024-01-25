package qingzhou.app.master.service;

import qingzhou.app.master.Main;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.AddModel;
import qingzhou.framework.api.FieldType;
import qingzhou.framework.api.ListModel;
import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelAction;
import qingzhou.framework.api.ModelBase;
import qingzhou.framework.api.ModelField;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;
import qingzhou.framework.util.ExceptionUtil;
import qingzhou.framework.util.FileUtil;

import java.io.File;
import java.util.Map;

@Model(name = "app", icon = "cube-alt",
        menuName = "Service", menuOrder = 1,
        nameI18n = {"应用", "en:App"},
        infoI18n = {"应用。",
                "en:App Management."})
public class App extends ModelBase implements AddModel {
    @ModelField(
            required = true,
            showToList = true,
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
            infoI18n = {"部署的应用可以从客户端上传，也可以从服务器端指定的位置读取。注：出于安全考虑，QingZhou 出厂设置禁用了文件上传功能，您可在“控制台安全”模块了解详情和进行相关的配置操作。",
                    "en:The deployed app can be uploaded from the client or read from a location specified on the server side. Note: For security reasons, QingZhou factory settings disable file upload, you can learn more and perform related configuration operations in the \"Console Security\" module."})
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
            if (FrameworkContext.SYS_APP_MASTER.equals(id) ||
                    FrameworkContext.SYS_APP_NODE_AGENT.equals(id)) {
                return "app.id.system";
            }
        }

        return super.validate(request, fieldName);
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
        if (Boolean.parseBoolean(p.remove("appFrom"))) {
            srcFile = FileUtil.newFile(p.remove("fromUpload"));
        } else {
            srcFile = new File(p.remove("filename"));
        }
        if (!srcFile.exists() || !srcFile.isFile()) {
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
            throw ExceptionUtil.unexpectedException("unknown app type");
        }

        String[] nodes = p.get("nodes").split(",");
        for (String node : nodes) {
            try {
                if (FrameworkContext.SYS_NODE_LOCAL.equals(node)) { // 安装到本地节点
                    Main.getFc().getAppManager().getApp(FrameworkContext.SYS_APP_NODE_AGENT).invoke(FrameworkContext.NODE_AGENT_APP_INSTALLER_MODEL, FrameworkContext.NODE_AGENT_INSTALL_APP_ACTION, request, response);
                } else {
                    // TODO：调用远端 node 上的app add
                }
            } catch (Exception e) { // todo 部分失败，如何显示到页面？
                response.setSuccess(false);
                response.setMsg(e.getMessage());
                e.printStackTrace();
            }
        }

        if(response.isSuccess()){
            p.put("id", appName);
            getDataStore().addData(request.getModelName(), appName, p);
        }
    }

    @Override
    public void delete(Request request, Response response) throws Exception {
        String appName = request.getId();
        Map<String, String> p = getDataStore().getDataById("app", appName);
        String[] nodes = p.get("nodes").split(",");
        for (String node : nodes) {
            try {
                if (FrameworkContext.SYS_NODE_LOCAL.equals(node)) { // 安装到本地节点
                    Main.getFc().getAppManager().getApp(FrameworkContext.SYS_APP_NODE_AGENT).invoke(FrameworkContext.NODE_AGENT_APP_INSTALLER_MODEL, FrameworkContext.NODE_AGENT_UNINSTALL_APP_ACTION, request, response);
                } else {
                    // TODO：调用远端 node 上的app delete
                }
            } catch (Exception e) { // todo 部分失败，如何显示到页面？
                response.setSuccess(false);
                e.printStackTrace();
            }
        }
        getDataStore().deleteDataById("app", appName);
    }

    @ModelAction(name = "target",
            icon = "location-arrow", forwardToPage = "target",
            nameI18n = {"管理", "en:Manage"}, showToList = true,
            infoI18n = {"转到此应用的管理页面。", "en:Go to the administration page for this app."})
    public void switchTarget(Request request, Response response) throws Exception {
    }

    @Override
    @ModelAction(name = ACTION_NAME_CREATE,
            showToListHead = true,
            icon = "plus-sign", forwardToPage = "form",
            nameI18n = {"部署", "en:Deploy"},
            infoI18n = {"在该节点上部署应用。", "en:Deploy the application on the node."})
    public void create(Request request, Response response) throws Exception {
        AddModel.super.create(request, response);
    }
}
