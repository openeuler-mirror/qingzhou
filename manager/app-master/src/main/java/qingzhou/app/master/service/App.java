package qingzhou.app.master.service;

import qingzhou.api.*;
import qingzhou.api.type.Createable;
import qingzhou.api.type.Deletable;
import qingzhou.api.type.Listable;
import qingzhou.app.master.MasterApp;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.RequestImpl;
import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.StringUtil;
import qingzhou.logger.Logger;

import java.io.File;
import java.util.*;
import java.util.stream.Stream;

@Model(code = qingzhou.deployer.App.SYS_MODEL_APP, icon = "cube-alt",
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
            name = {"使用上传", "en:Enable Upload"},
            info = {"安装的应用可以从客户端上传，也可以从服务器端指定的位置读取。",
                    "en:The installed app can be uploaded from the client or read from a location specified on the server side."})
    public boolean appFrom = false;

    @ModelField(
            list = true,
            name = {"应用位置", "en:Application File"},
            info = {"服务器上应用程序的位置，通常是应用的程序包，注：须为 *.jar 类型的文件。",
                    "en:The location of the application on the server, usually the app package, Note: Must be a *.jar file."})
    public String filename;

    @ModelField(
            name = {"上传应用", "en:Upload Application"},
            info = {"上传一个应用文件到服务器，文件须是 *.jar 或 *.zip 类型的 Qingzhou 应用文件，否则可能会导致安装失败。",
                    "en:Upload an application file to the server, the file must be a *.jar type qingzhou application file, otherwise the installation may fail."})
    public String fromUpload;

    @ModelField(
            list = true,
            name = {"节点", "en:Node"},
            info = {"选择安装应用的节点。", "en:Select the node where you want to install the application."})
    public String nodes;

    @ModelField(
            list = true,
            name = {"应用版本", "en:Instance Version"},
            info = {"此应用的版本。", "en:The version of this app."})
    public String version;

    @ModelField(
            list = true,
            name = {"类型", "en:Type"},
            info = {"此应用的类型。", "en:The type of this app."})
    public String type;

    @Override
    public void init() {
        getAppContext().addI18n("app.id.not.exist", new String[]{"应用文件不存在", "en:The app file does not exist"});
        getAppContext().addI18n("app.type.unknown", new String[]{"未知的应用类型", "en:Unknown app type"});
    }

    @Override
    public Options options(Request request, String fieldName) {
        if ("nodes".equals(fieldName)) {
            String userName = request.getUser();
            List<Option> nodeList = new ArrayList<>();
            nodeList.add(Option.of(qingzhou.deployer.App.SYS_NODE_LOCAL));  // 将SYS_NODE_LOCAL始终添加到列表的第一位
            Set<String> nodeSet = new HashSet<>();
            try {
                if ("qingzhou".equals(userName)) {
                    List<Map<String, String>> nodes = getDataStore().getAllData("node");
                    nodes.stream()
                            .map(node -> node.get("id"))
                            .filter(Objects::nonNull)
                            .forEach(nodeSet::add);
                } else {
                    Map<String, String> user = getDataStore().getDataById("user", userName);
                    if (user != null) {
                        Stream.of(user.getOrDefault("nodes", "").split(","))
                                .map(String::trim)
                                .filter(StringUtil::notBlank)
                                .forEach(nodeSet::add);
                    }
                }
                nodeSet.remove(qingzhou.deployer.App.SYS_NODE_LOCAL);
                nodeSet.stream().map(Option::of).forEach(nodeList::add);
            } catch (Exception e) {
                MasterApp.getService(Logger.class).error(e.getMessage(), e);
            }

            return () -> nodeList;
        }

        return super.options(request, fieldName);
    }

    @ModelAction(name = Createable.ACTION_NAME_ADD,
            name = {"安装", "en:Install"},
            info = {"按配置要求安装应用到指定的节点。", "en:Install the app to the specified node as required."})
    public void add(Request req, Response response) throws Exception {
        RequestImpl request = (RequestImpl) req;
        Map<String, String> p = MasterApp.prepareParameters(request, getAppContext());
        File srcFile;
        if (Boolean.parseBoolean(p.remove("appFrom"))) {
            srcFile = FileUtil.newFile(p.remove("fromUpload"));
        } else {
            srcFile = new File(p.remove("filename"));
        }
        if (!srcFile.exists() || !srcFile.isFile()) {
            response.setSuccess(false);
            String msg = getAppContext().getAppMetadata().getI18n(request.getLang(), "app.id.not.exist");
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
            String msg = getAppContext().getAppMetadata().getI18n(request.getLang(), "app.type.unknown");
            response.setMsg(msg);
            return;
        }

        String[] nodes = p.get("nodes") == null ? new String[0] : p.get("nodes").split(",");
        request.setModelName(qingzhou.deployer.App.SYS_MODEL_APP_INSTALLER);
        request.setActionName(qingzhou.deployer.App.SYS_ACTION_INSTALL_APP);
        try {
            for (String node : nodes) {
                try {
                    if (qingzhou.deployer.App.SYS_NODE_LOCAL.equals(node)) { // 安装到本地节点
                        MasterApp.getService(Deployer.class).getApp(qingzhou.deployer.App.SYS_APP_NODE_AGENT).invokeDirectly(request, response);
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
            request.setModelName(qingzhou.deployer.App.SYS_MODEL_APP);
            request.setActionName(Createable.ACTION_NAME_ADD);
        }

        p.put(Listable.FIELD_NAME_ID, appName);
        getDataStore().addData(qingzhou.deployer.App.SYS_MODEL_APP, appName, p);
    }

    @ModelAction(name = qingzhou.deployer.App.SYS_ACTION_MANAGE_PAGE,
            name = {"管理", "en:Manage"},
            info = {"转到此应用的管理页面。", "en:Go to the administration page for this app."})
    public void switchTarget(Request request, Response response) throws Exception {
    }

    @ModelAction(
            name = Deletable.ACTION_NAME_DELETE,
            batch = true,
            name = {"删除", "en:Delete"},
            info = {"删除这个组件，该组件引用的其它组件不会被删除。注：请谨慎操作，删除后不可恢复。",
                    "en:Delete this component, other components referenced by this component will not be deleted. Note: Please operate with caution, it cannot be recovered after deletion."})
    public void delete(Request req, Response response) throws Exception {
        RequestImpl request = (RequestImpl) req;
        String appName = request.getId();
        Map<String, String> p = getDataStore().getDataById("app", appName);
        String[] nodes = p.get("nodes").split(",");

        request.setModelName(qingzhou.deployer.App.SYS_MODEL_APP_INSTALLER);
        request.setActionName(qingzhou.deployer.App.SYS_ACTION_UNINSTALL_APP);
        try {
            for (String node : nodes) {
                try {
                    if (qingzhou.deployer.App.SYS_NODE_LOCAL.equals(node)) { // 安装到本地节点
                        MasterApp.getService(Deployer.class).getApp(qingzhou.deployer.App.SYS_APP_NODE_AGENT).invokeDirectly(request, response);
                    } else {
                        // TODO：调用远端 node 上的app delete
                    }
                } catch (Exception e) { // todo 部分失败，如何显示到页面？
                    response.setSuccess(false);
                    response.setMsg(e.getMessage());
                }
            }
        } finally {
            request.setModelName(qingzhou.deployer.App.SYS_MODEL_APP);
            request.setActionName(Deletable.ACTION_NAME_DELETE);
        }
        getDataStore().deleteDataById("app", appName);
    }
}