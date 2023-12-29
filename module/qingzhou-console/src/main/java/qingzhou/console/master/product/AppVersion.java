package qingzhou.console.master.product;

import qingzhou.api.console.FieldType;
import qingzhou.api.console.Model;
import qingzhou.api.console.ModelField;
import qingzhou.api.console.data.Datas;
import qingzhou.api.console.data.Request;
import qingzhou.api.console.data.Response;
import qingzhou.api.console.model.AddModel;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.console.master.MasterModelBase;
import qingzhou.console.util.Constants;
import qingzhou.console.util.FileUtil;
import qingzhou.console.util.StringUtil;
import qingzhou.framework.app.I18n;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Model(name = Constants.MODEL_NAME_appversion, icon = "rss",
        menuName = "Product", menuOrder = 1,
        nameI18n = {"产品包", "en:App Package"},
        infoI18n = {"管理产品的版本信息。",
                "en:Manage version information for products."})
public class AppVersion extends MasterModelBase implements AddModel {
    @ModelField(
            required = true, showToList = true,
            nameI18n = {"名称", "en:Name"},
            infoI18n = {"实例名称。", "en:Instance Name"})
    public String id;

    @ModelField(
            required = true,
            effectiveOnEdit = false,
            type = FieldType.upload,
            nameI18n = {"应用来源", "en:Application Source"},
            infoI18n = {"部署的应用可以从客户端上传，也可以从服务器端指定的位置读取。注：出于安全考虑，QingZhou 出厂设置禁用了文件上传功能，您可在“控制台安全”模块了解详情和进行相关的配置操作。",
                    "en:The deployed app can be uploaded from the client or read from a location specified on the server side. Note: For security reasons, QingZhou factory settings disable file upload, you can learn more and perform related configuration operations in the \"Console Security\" module."})
    public String appFrom = Constants.FILE_FROM_SERVER;

    @ModelField(
            showToList = true,
            effectiveWhen = "appFrom=" + Constants.FILE_FROM_SERVER,
            effectiveOnEdit = false,
            required = true,
            notSupportedCharacters = "#",
            unique = true,
            maxLength = 255,// for #NC-1418 及其它文件目录操作的，文件长度不能大于 255
            nameI18n = {"应用位置", "en:Application File"},
            infoI18n = {"服务器上应用程序的位置，通常是应用的程序包，如 *.war *.ear 等 Java EE 标准类型的文件。",
                    "en:The location of the application on the server, usually the application package, such as *.war *.ear and other Java EE standard type files."})
    public String filename;

    @ModelField(
            type = FieldType.file,
            effectiveWhen = "appFrom=" + Constants.FILE_FROM_UPLOAD,
            effectiveOnEdit = false,
            showToEdit = false,
            notSupportedCharacters = "#",
            required = true,
            unique = true,
            nameI18n = {"上传应用", "en:Upload Application"},
            infoI18n = {"上传一个应用文件到服务器，文件须是 *.war *.ear 等 Java EE 标准类型的文件，否则可能会导致部署失败。",
                    "en:Upload an application file to the server, the file must be a Java EE standard type file such as *.war *.ear, otherwise, the deployment may fail."})
    public String fromUpload;// 必需一致于: com.tongweb.server.util.Constants.FILE_FROM_UPLOAD

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
        // 确定文件
        File srcFile;
        if (Constants.FILE_FROM_UPLOAD.equals(p.get("appFrom"))) {
            srcFile = FileUtil.newFile(p.remove(Constants.FILE_FROM_UPLOAD));
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
            FileUtil.unZipToDir(srcFile, ConsoleWarHelper.getAppDir());
        } catch (IOException e) {
            e.printStackTrace();
            response.setSuccess(false);
            return;
        }

        p.put("filename", srcFileName);
        if (ConsoleWarHelper.getAppInfoManager().addApp(srcFile)) {
            getDataStore().addData(request.getModelName(), p.get("id"), p);
        } else {
            FileUtil.delete(FileUtil.newFile(ConsoleWarHelper.getAppDir(), srcFileName));
        }
    }

    @Override
    public void delete(Request request, Response response) throws Exception {
        String id = request.getId();
        Map<String, String> app = getDataStore().getDataById(Constants.MODEL_NAME_appversion, id);
        if (app == null || app.isEmpty()) {
            return;
        }
        List<Map<String, String>> instanceList = getDataStore().getDataByKey(Constants.MODEL_NAME_node, "app", id);
        if (instanceList != null && instanceList.size() > 0) {
            String thisModel = I18n.getString(Constants.QINGZHOU_MASTER_APP_NAME, "model." + request.getModelName()) + "(" + id + ")";
            String detail = String.format(I18n.getString(Constants.QINGZHOU_MASTER_APP_NAME, "validator.hasRefModel"), thisModel, I18n.getString(Constants.QINGZHOU_MASTER_APP_NAME, "model." + Constants.MODEL_NAME_node));
            response.setSuccess(false);
            response.setMsg(detail);
            return;
        }
        String filename = app.get("filename");
        File appDir = FileUtil.newFile(ConsoleWarHelper.getAppDir(), filename);
        if (appDir.exists()) {
            try {
                FileUtil.forceDelete(appDir);
            } catch (IOException e) {
                e.printStackTrace();
                response.setSuccess(false);
                return;
            }
        }

        if (ConsoleWarHelper.getAppInfoManager().removeApp(filename)) {
            getDataStore().deleteDataById(request.getModelName(), id);
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

        List<String> appNames = new ArrayList<>(ConsoleWarHelper.getAppInfoManager().getApps());
        List<Map<String, String>> apps = getDataStore().getAllData(Constants.MODEL_NAME_appversion);
        if (apps != null && apps.size() > 0) {
            for (Map<String, String> app : apps) {
                String filename = app.getOrDefault("filename", "");
                if (appNames.contains(filename)) {
                    appNames.add(app.get("id"));
                    appNames.remove(filename);
                }
            }
        }
        Datas dataInfo = response.modelData();
        dataInfo.setTotalSize(appNames.size());
        dataInfo.setPageSize(pageSize());
        dataInfo.setPageNum(pageNum);

        int start = (pageNum - 1) * pageSize();
        int end = appNames.size() < pageSize() ? appNames.size() : (start + pageSize());
        for (int i = start; i < end; i++) {
            String appName = appNames.get(i);
            response.modelData().addDataObject(buildApp(appName), getConsoleContext());
        }
    }

    private AppVersion buildApp(String appName) {
        AppVersion appPublish = new AppVersion();
        appPublish.id = appName;
        if (appName.equals(Constants.QINGZHOU_DEFAULT_APP_NAME)) {
            appPublish.type = Constants.QINGZHOU_DEFAULT_APP_NAME;
            appPublish.filename = "";
        } else {
            try {
                Map<String, String> app = getDataStore().getDataById(Constants.MODEL_NAME_appversion, appName);
                if (app != null && !app.isEmpty()) {
                    appPublish.version = app.getOrDefault("version", "");
                    appPublish.type = app.getOrDefault("type", "");
                    appPublish.filename = app.getOrDefault("filename", "");
                }
            } catch (Exception ignored) {
            }
        }

        return appPublish;
    }

    @Override
    public List<String> getAllDataId(String modelName) throws Exception {
        return new ArrayList<>(ConsoleWarHelper.getAppInfoManager().getApps());
    }

    @Override
    public String validate(Request request, String fieldName) {
        String appFrom = request.getParameter("appFrom");

        if ("filename".equals(fieldName)) {
            if (StringUtil.isBlank(fieldName)) {
                if (Constants.FILE_FROM_SERVER.equals(appFrom)) {
                    return null;
                }
            }
        }

        if ("fromUpload".equals(fieldName)) {
            if (StringUtil.isBlank(fieldName)) {
                if (Constants.FILE_FROM_UPLOAD.equals(appFrom)) {
                    return null;
                }
            }
        }

        return super.validate(request, fieldName);
    }
}
