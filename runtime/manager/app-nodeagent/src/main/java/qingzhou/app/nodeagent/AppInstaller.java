package qingzhou.app.nodeagent;

import qingzhou.api.*;
import qingzhou.app.AppInfo;
import qingzhou.app.AppManager;
import qingzhou.engine.util.FileUtil;

import java.io.File;

@Model(name = AppInfo.SYS_MODEL_APP_INSTALLER, icon = "",
        showToMenu = false,
        nameI18n = {"应用安装器", "en:App Installer"},
        infoI18n = {"执行管理节点下发的应用安装、卸载等指令。",
                "en:Execute the commands issued by the management node to install and uninstall applications."})
public class AppInstaller extends ModelBase {

    @ModelAction(name = AppInfo.SYS_ACTION_INSTALL_APP,
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
            FileUtil.copyFileOrDirectory(srcFile, FileUtil.newFile(app, srcFileName));
        } else if (srcFileName.endsWith(".zip")) {
            int index = srcFileName.lastIndexOf(".");
            String appName = srcFileName.substring(0, index);
            app = FileUtil.newFile(getAppsDir(), appName);
            FileUtil.unZipToDir(srcFile, app);
        } else {
            throw new IllegalArgumentException("unknown app type");
        }

        AppManager appManager = NodeAgentApp.getService(AppManager.class);
        appManager.installApp(app);
    }

    @ModelAction(name = AppInfo.SYS_ACTION_UNINSTALL_APP,
            nameI18n = {"卸载应用", "en:UnInstall App"},
            infoI18n = {"从该节点上卸载应用。", "en:Uninstall the app from the node."})
    public void unInstallApp(Request request, Response response) throws Exception {
        NodeAgentApp.getService(AppManager.class).unInstallApp(request.getId());
        FileUtil.forceDelete(FileUtil.newFile(getAppsDir(), request.getId()));
    }

    private File getAppsDir() {
        return FileUtil.newFile(NodeAgentApp.getFc().getInstanceDir(), "apps");
    }
}
