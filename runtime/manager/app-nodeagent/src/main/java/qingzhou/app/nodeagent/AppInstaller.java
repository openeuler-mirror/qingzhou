package qingzhou.app.nodeagent;

import qingzhou.api.*;
import qingzhou.deployer.App;
import qingzhou.deployer.Deployer;
import qingzhou.engine.util.FileUtil;

import java.io.File;

@Model(code = App.SYS_MODEL_APP_INSTALLER, icon = "",
        hidden = true,
        name = {"应用安装器", "en:App Installer"},
        info = {"执行管理节点下发的应用安装、卸载等指令。",
                "en:Execute the commands issued by the management node to install and uninstall applications."})
public class AppInstaller extends ModelBase {

    @ModelAction(name = App.SYS_ACTION_INSTALL_APP,
            name = {"安装应用", "en:Install App"},
            info = {"在该节点上安装应用。", "en:Install the application on the node."})
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

        Deployer deployer = NodeAgentApp.getService(Deployer.class);
        deployer.installApp(app);
    }

    @ModelAction(name = App.SYS_ACTION_UNINSTALL_APP,
            name = {"卸载应用", "en:UnInstall App"},
            info = {"从该节点上卸载应用。", "en:Uninstall the app from the node."})
    public void unInstallApp(Request request, Response response) throws Exception {
        NodeAgentApp.getService(Deployer.class).unInstallApp(request.getId());
        FileUtil.forceDelete(FileUtil.newFile(getAppsDir(), request.getId()));
    }

    private File getAppsDir() {
        return FileUtil.newFile(NodeAgentApp.getFc().getInstanceDir(), "apps");
    }
}
