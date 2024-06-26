package qingzhou.app.instance;

import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelBase;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.deployer.Deployer;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.Utils;

import java.io.File;

@Model(code = "appinstaller",
        hidden = true,
        name = {"应用安装器", "en:App Installer"},
        info = {"执行管理节点下发的应用安装、卸载等指令。",
                "en:Execute the commands issued by the management node to install and uninstall applications."})
public class AppInstaller extends ModelBase {
    @Override
    public void start() {
        appContext.addI18n("app.not.found", new String[]{"应用文件未找到", "en:File Not Found"});
    }

    @ModelAction(
            name = {"安装应用", "en:Install App"},
            info = {"在该实例上安装应用。", "en:Install the application on the instance."})
    public void installApp(Request request, Response response) throws Exception {
        File srcFile;
        if (Boolean.parseBoolean(request.getParameter("appFrom"))) {
            srcFile = Utils.newFile(request.getParameter("fromUpload"));
        } else {
            srcFile = new File(request.getParameter("filename"));
        }
        if (!srcFile.exists() || !srcFile.isFile()) {
            response.setSuccess(false);
            response.setMsg(appContext.getI18n(request.getLang(), "app.not.found"));
            return;
        }

        String srcFileName = srcFile.getName();
        File app;
        String id = request.getId();
        if (srcFile.isDirectory()) {
            app = Utils.newFile(getAppsDir(), id == null ? srcFileName : id);
            Utils.copyFileOrDirectory(srcFile, app);
        } else if (srcFileName.endsWith(".jar")) {
            int index = srcFileName.lastIndexOf(".");
            String appName = srcFileName.substring(0, index);
            app = Utils.newFile(getAppsDir(), id == null ? appName : id);
            Utils.copyFileOrDirectory(srcFile, Utils.newFile(app, srcFileName));
        } else if (srcFileName.endsWith(".zip")) {
            int index = srcFileName.lastIndexOf(".");
            String appName = srcFileName.substring(0, index);
            app = Utils.newFile(getAppsDir(), id == null ? appName : id);
            Utils.unZipToDir(srcFile, app);
        } else {
            throw new IllegalArgumentException("unknown app type");
        }

        InstanceApp.getService(Deployer.class).installApp(app);
    }

    @ModelAction(
            name = {"卸载应用", "en:UnInstall App"},
            info = {"从该实例上卸载应用。", "en:Uninstall the app from the instance."})
    public void unInstallApp(Request request, Response response) throws Exception {
        InstanceApp.getService(Deployer.class).unInstallApp(request.getId());
        Utils.forceDelete(Utils.newFile(getAppsDir(), request.getId()));
    }

    private File getAppsDir() {
        return Utils.newFile(InstanceApp.getService(ModuleContext.class).getInstanceDir(), "apps");
    }
}
