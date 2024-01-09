package qingzhou.app.master.service;

import qingzhou.app.master.Main;
import qingzhou.framework.AppManager;
import qingzhou.framework.api.AddModel;
import qingzhou.framework.api.FieldType;
import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelBase;
import qingzhou.framework.api.ModelField;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;
import qingzhou.framework.console.ConsoleConstants;
import qingzhou.framework.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import qingzhou.framework.api.ModelAction;

@Model(name = "app", icon = "rss",
        menuName = "Service", menuOrder = 1,
        nameI18n = {"应用管理", "en:App Management"},
        infoI18n = {"应用管理。",
                "en:App Management."})
public class App extends ModelBase implements AddModel {
    @ModelField(
            required = true, showToList = true,
            nameI18n = {"名称", "en:Name"},
            infoI18n = {"应用名称。", "en:Instance Name"})
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
            infoI18n = {"上传一个应用文件到服务器，文件须是 *.war *.ear 等 Java EE 标准类型的文件，否则可能会导致部署失败。",
                    "en:Upload an application file to the server, the file must be a Java EE standard type file such as *.war *.ear, otherwise, the deployment may fail."})
    public String fromUpload;

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

    @ModelAction(name = ACTION_NAME_ADD,
            icon = "save",
            nameI18n = {"添加", "en:Add"},
            infoI18n = {"按配置要求创建一个模块。", "en:Create a module as configured."})
    @Override
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
        int index = srcFileName.lastIndexOf(".");
        if (index > -1) {
            srcFileName = srcFileName.substring(0, index);
        }
        try {
            FileUtil.copyFileOrDirectory(srcFile, getApps());
        } catch (IOException e) {
            e.printStackTrace();
            response.setSuccess(false);
            return;
        }
        File app = FileUtil.newFile(getApps(), srcFileName);
        p.put("filename", srcFileName);

        try {
            getAppManager().installApp(srcFileName, false, app);
            getDataStore().addData(request.getModelName(), p.get("id"), p);
        } catch (Exception e) {
            e.printStackTrace();
            FileUtil.delete(app);
            response.setSuccess(false);
            return;
        }
    }

    @ModelAction(
            name = ACTION_NAME_DELETE,
            showToList = true, supportBatch = true,
            icon = "trash",
            nameI18n = {"删除", "en:Delete"},
            infoI18n = {"删除这个组件，该组件引用的其它组件不会被删除。注：请谨慎操作，删除后不可恢复。",
                    "en:Delete this component, other components referenced by this component will not be deleted. Note: Please operate with caution, it cannot be recovered after deletion."})
    @Override
    public void delete(Request request, Response response) throws Exception {
        String id = request.getId();
        Map<String, String> appInfo = getDataStore().getDataById("app", id);
        if (appInfo == null || appInfo.isEmpty()) {
            return;
        }

        String filename = appInfo.get("filename");
        File app = FileUtil.newFile(getApps(), filename);
        if (app.exists()) {
            try {
                FileUtil.forceDelete(app);
            } catch (IOException e) {
                e.printStackTrace();
                response.setSuccess(false);
                return;
            }
        }

        try {
            getAppManager().uninstallApp(filename);
            getDataStore().deleteDataById(request.getModelName(), id);
        } catch (Exception e) {
            e.printStackTrace();
            response.setSuccess(false);
        }
    }

    @ModelAction(name = ACTION_NAME_LIST,
            icon = "list", forwardToPage = "list",
            nameI18n = {"列表", "en:List"},
            infoI18n = {"展示该类型的所有组件数据或界面。", "en:Show all component data or interfaces of this type."})
    @Override
    public void list(Request request, Response response) throws Exception {
        int pageNum = 1;
        String pageNumParameter = request.getParameter(PARAMETER_PAGE_NUM);
        if (pageNumParameter != null) {
            try {
                pageNum = Integer.parseInt(pageNumParameter);
            } catch (NumberFormatException ignored) {
            }
        }

        List<String> appNames = new ArrayList<>(getAppManager().getApps());
        if (getDataStore() != null) {
            List<Map<String, String>> apps = getDataStore().getAllData("app");
            if (apps != null && apps.size() > 0) {
                for (Map<String, String> app : apps) {
                    String filename = app.getOrDefault("filename", "");
                    if (appNames.contains(filename)) {
                        appNames.add(app.get("id"));
                        appNames.remove(filename);
                    }
                }
            }
        }
        appNames.remove(ConsoleConstants.MASTER_APP_NAME);// master系统应用不显示

        List<Map<String, String>> dataInfo = response.getDataList();
        response.setTotalSize(appNames.size());
        response.setPageSize(pageSize());
        response.setPageNum(pageNum);

        int start = (pageNum - 1) * pageSize();
        int end = appNames.size() < pageSize() ? appNames.size() : (start + pageSize());
        for (int i = start; i < end; i++) {
            String appName = appNames.get(i);
            if (getDataStore() != null) {
                dataInfo.add(getDataStore().getDataById("app", appName));
            }
        }
    }

    private AppManager getAppManager() {
        return Main.getFC().getAppManager();
    }

    public File getApps() {
        return FileUtil.newFile(getAppContext().getDomain(), "apps");
    }
}
