package qingzhou.app.system;

import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelBase;
import qingzhou.api.Request;
import qingzhou.crypto.CryptoService;
import qingzhou.deployer.App;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.DeployerConstants;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.FileUtil;

import java.io.File;
import java.io.IOException;

@Model(code = DeployerConstants.MODEL_AGENT,
        hidden = true,
        name = {"实例代理", "en:Agent"},
        info = {"执行管理节点下发的应用安装、卸载等指令。",
                "en:Execute the commands issued by the management node to install and uninstall applications."})
public class Agent extends ModelBase {
    @Override
    public void start() {
        appContext.addI18n("app.exists", new String[]{"应用已存在，请更换为其它的应用名后重试",
                "en:If the application already exists, please change it to another application name and try again"});
        appContext.addI18n("app.not.found", new String[]{"应用文件未找到",
                "en:The app file was not found"});
        appContext.addI18n("app.type.unknown", new String[]{"应用文件类型无法识别",
                "en:The app file type is not recognized"});
    }

    @ModelAction(
            code = DeployerConstants.ACTION_INSTALL,
            name = {"安装应用", "en:Install App"},
            info = {"在该实例上安装应用。", "en:Install the application on the instance."})
    public void install(Request request) throws Exception {
        String fileId = request.getParameter(DeployerConstants.INSTALLER_PARAMETER_FILE_ID);
        File srcFile = new File(fileId);
        if (!srcFile.exists()) {
            File uploadDir = new File(appContext.getTemp(), fileId);
            File[] listFiles = uploadDir.listFiles();
            if (listFiles == null || listFiles.length == 0) {
                request.getResponse().setSuccess(false);
                request.getResponse().setMsg(appContext.getI18n("app.not.found"));
                return;
            }
            srcFile = listFiles[0];
        }

        String fileName = srcFile.getName();
        File app;
        if (srcFile.isDirectory()) {
            app = FileUtil.newFile(getAppsDir(), fileName);
            FileUtil.copyFileOrDirectory(srcFile, app);
        } else if (fileName.endsWith(".jar")) {
            int index = fileName.lastIndexOf(".");
            String appName = fileName.substring(0, index);
            app = FileUtil.newFile(getAppsDir(), appName);
            FileUtil.copyFileOrDirectory(srcFile, FileUtil.newFile(app, fileName));
        } else if (fileName.endsWith(".zip")) {
            int index = fileName.lastIndexOf(".");
            String appName = fileName.substring(0, index);
            app = FileUtil.newFile(getAppsDir(), appName);
            FileUtil.unZipToDir(srcFile, app);
        } else {
            request.getResponse().setSuccess(false);
            request.getResponse().setMsg(appContext.getI18n("app.type.unknown"));
            return;
        }

        Deployer deployer = Main.getService(Deployer.class);
        App deployerApp = deployer.getApp(app.getName());
        if (deployerApp != null) {
            request.getResponse().setSuccess(false);
            request.getResponse().setMsg(appContext.getI18n("app.exists"));
            return;
        }

        try {
            deployer.installApp(app);
        } catch (Exception e) {
            FileUtil.forceDelete(app);
            throw e;
        }
    }

    @ModelAction(
            code = DeployerConstants.ACTION_UNINSTALL,
            name = {"卸载应用", "en:UnInstall App"},
            info = {"从该实例上卸载应用。", "en:Uninstall the app from the instance."})
    public void uninstall(Request request) throws Exception {
        Main.getService(Deployer.class).unInstallApp(request.getId());
        FileUtil.forceDelete(FileUtil.newFile(getAppsDir(), request.getId()));
    }

    private File getAppsDir() {
        return FileUtil.newFile(Main.getService(ModuleContext.class).getInstanceDir(), "apps");
    }

    @ModelAction(
            code = DeployerConstants.ACTION_UPLOAD,
            name = {"上传文件", "en:Upload File"},
            info = {"应用模块表单文件上传。", "en:Upload the application module form file."})
    public void upload(Request request) throws IOException {
        String fileId = request.getParameter(DeployerConstants.INSTALLER_PARAMETER_FILE_ID);
        String fileName = request.getParameter(DeployerConstants.INSTALLER_PARAMETER_FILE_NAME);
        byte[] fileBytes = Main.getService(CryptoService.class).getBase64Coder().decode(
                request.getParameter(DeployerConstants.INSTALLER_PARAMETER_FILE_BYTES));

        File file = FileUtil.newFile(appContext.getTemp(), fileId, fileName);
        FileUtil.writeFile(file, fileBytes);
    }
}