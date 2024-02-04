package qingzhou.app.node;

import qingzhou.framework.AppManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelAction;
import qingzhou.framework.api.ModelBase;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;
import qingzhou.framework.util.ExceptionUtil;
import qingzhou.framework.util.FileUtil;

import java.io.File;

@Model(name = FrameworkContext.SYS_MODEL_APP_INSTALLER, icon = "",
        showToMenu = false,
        nameI18n = {"应用安装器", "en:App Installer"},
        infoI18n = {"执行管理节点下发的应用安装、卸载等指令。",
                "en:Execute the commands issued by the management node to install and uninstall applications."})
public class AppInstaller extends ModelBase {

    @ModelAction(name = FrameworkContext.SYS_ACTION_INSTALL,
            nameI18n = {"安装应用", "en:Install App"},
            infoI18n = {"在该节点上安装应用。", "en:Install the application on the node."})
    public void installApp(Request request, Response response) throws Exception {
        File srcFile;
        if (Boolean.parseBoolean(request.getParameter("appFrom"))) {
            srcFile = FileUtil.newFile(request.getParameter("fromUpload"));
        } else {
            srcFile = new File(request.getParameter("filename"));
        }
        if (!srcFile.exists() || !srcFile.isFile()) {
            response.setSuccess(false);
            response.setMsg("File Not Found.");
            return;
        }

        String srcFileName = srcFile.getName();
        File app;
        if (srcFile.isDirectory()) {
            app = FileUtil.newFile(getAppsDir(), srcFileName);
            FileUtil.copyFileOrDirectory(srcFile, app);
        } else if (srcFileName.endsWith(".jar")) {
            int index = srcFileName.lastIndexOf(".");
            String appName = srcFileName.substring(0, index);
            app = FileUtil.newFile(getAppsDir(), appName);
            FileUtil.copyFileOrDirectory(srcFile, FileUtil.newFile(app, "lib", srcFileName));
        } else if (srcFileName.endsWith(".zip")) {
            int index = srcFileName.lastIndexOf(".");
            String appName = srcFileName.substring(0, index);
            app = FileUtil.newFile(getAppsDir(), appName);
            FileUtil.unZipToDir(srcFile, app);
        } else {
            throw ExceptionUtil.unexpectedException("unknown app type");
        }

        AppManager appManager = Main.getFc().getAppManager();
        appManager.installApp(app);
    }

    @ModelAction(name = FrameworkContext.SYS_ACTION_UNINSTALL,
            nameI18n = {"卸载应用", "en:UnInstall App"},
            infoI18n = {"从该节点上卸载应用。", "en:Uninstall the app from the node."})
    public void unInstallApp(Request request, Response response) throws Exception {
        Main.getFc().getAppManager().unInstallApp(request.getId());
        FileUtil.forceDelete(FileUtil.newFile(getAppContext().getDomain(), "apps", request.getId()));
    }

    private File getAppsDir() {
        File apps = FileUtil.newFile(getAppContext().getDomain(), "apps");
        if (!apps.exists()) {
            FileUtil.mkdirs(apps);
        }

        return apps;
    }
}
