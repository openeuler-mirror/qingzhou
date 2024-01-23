package qingzhou.app.node.service;

import qingzhou.app.node.Main;
import qingzhou.framework.AppManager;
import qingzhou.framework.api.AddModel;
import qingzhou.framework.api.ModelAction;
import qingzhou.framework.api.ModelBase;
import qingzhou.framework.api.ModelField;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;
import qingzhou.framework.util.ExceptionUtil;
import qingzhou.framework.util.FileUtil;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class App extends ModelBase implements AddModel { // todo 接收 master 的安装和卸载应用等请求
    @ModelField(
            showToList = true,
            disableOnCreate = true, disableOnEdit = true,
            nameI18n = {"名称", "en:Name"},
            infoI18n = {"应用名称。", "en:App Name"})
    public String id;

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
        File app;
        if (srcFile.isDirectory()) {
            appName = srcFileName;
            app = FileUtil.newFile(getAppsDir(), appName);
            FileUtil.copyFileOrDirectory(srcFile, app);
        } else if (srcFileName.endsWith(".jar")) {
            int index = srcFileName.lastIndexOf(".");
            appName = srcFileName.substring(0, index);
            app = FileUtil.newFile(getAppsDir(), appName);
            FileUtil.copyFileOrDirectory(srcFile, FileUtil.newFile(app, "lib", srcFileName));
        } else if (srcFileName.endsWith(".zip")) {
            int index = srcFileName.lastIndexOf(".");
            appName = srcFileName.substring(0, index);
            app = FileUtil.newFile(getAppsDir(), appName);
            FileUtil.unZipToDir(srcFile, app);
        } else {
            throw ExceptionUtil.unexpectedException("unknown app type");
        }

        Main.getFc().getAppDeployer().installApp(appName, app);
    }

    @Override
    public void delete(Request request, Response response) throws Exception {
        Main.getFc().getAppDeployer().unInstallApp(request.getId());
    }

    @Override
    public void update(Request request, Response response) throws Exception {
        // TODO 是否支持更新？
        delete(request, response);
        add(request, response);
    }

    @Override
    public void list(Request request, Response response) throws Exception {
        AppManager appManager = Main.getFc().getAppManager();
        Set<String> apps = appManager.getApps();
        for (String appName : apps) {
            App app = new App();
            app.id = appName;
            response.addDataObject(app);
        }
    }

    @Override
    @ModelAction(name = ACTION_NAME_CREATE,
            icon = "plus-sign", forwardToPage = "form",
            nameI18n = {"创建", "en:Create"},
            infoI18n = {"获得创建该组件的默认数据或界面。", "en:Get the default data or interface for creating this component."})
    public void create(Request request, Response response) throws Exception {

    }

    @Override
    @ModelAction(name = ACTION_NAME_EDIT,
            icon = "edit", forwardToPage = "form",
            nameI18n = {"编辑", "en:Edit"},
            infoI18n = {"获得可编辑的数据或界面。", "en:Get editable data or interfaces."})
    public void edit(Request request, Response response) throws Exception {

    }

    public File getAppsDir() {
        File apps = FileUtil.newFile(getAppContext().getDomain(), "apps");
        if (!apps.exists()) {
            FileUtil.mkdirs(apps);
        }

        return apps;
    }
}
