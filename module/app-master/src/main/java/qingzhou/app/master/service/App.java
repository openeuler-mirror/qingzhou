package qingzhou.app.master.service;

import qingzhou.app.master.Main;
import qingzhou.framework.AppManager;
import qingzhou.framework.api.AddModel;
import qingzhou.framework.api.FieldType;
import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelAction;
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
        File app = FileUtil.newFile(getApps(), srcFile.getName());
        try {
            FileUtil.copyFileOrDirectory(srcFile, app);
        } catch (IOException e) {
            e.printStackTrace();
            response.setSuccess(false);
            return;
        }
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

    @ModelAction(name = "start",
            effectiveWhen = "running=false",
            icon = "play",
            nameI18n = {"启动", "en:Start"},
            infoI18n = {"启动操作可能因为超时而提示失败，可在一段时间后刷新状态以确认是否启动成功。", "en:The startup operation may fail due to timeout. You can refresh the status after a period of time to confirm whether the startup is successful."})
    public void start(Request request, Response response) throws Exception {

    }

    @ModelAction(name = "stop",
            effectiveWhen = "running=true",
            icon = "stop",
            nameI18n = {"停止", "en:Stop"},
            infoI18n = {"停止这个应用，停止后该应用不再对外提供服务，继续访问该应用将得到404响应码。注：该操作仅对Web类型的应用有效。",
                    "en:Stop the application. After stopping, the application will no longer provide services to the outside world. If you continue to access the application, you will get a 404 response code. Note: This operation is only valid for web-type applications."})
    public void stop(Request request, Response response) throws Exception {

    }

    @ModelAction(name = "target",
            icon = "location-arrow", forwardToPage = "target",
            nameI18n = {"管理", "en:Manage"},
            effectiveWhen = "running=true",
            infoI18n = {"转到此实例的管理页面。", "en:Go to the administration page for this instance."})
    public void switchTarget(Request request, Response response) throws Exception {

    }

    private AppManager getAppManager() {
        return Main.getFC().getAppManager();
    }

    public File getApps() {
        File apps = FileUtil.newFile(Main.getFC().getDomain(), "apps");
        if (!apps.exists()) {
            FileUtil.mkdirs(apps);
        }

        return apps;
    }
}
