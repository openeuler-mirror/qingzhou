package qingzhou.app.system;

import qingzhou.api.*;
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
    @ModelField(
            type = FieldType.bool,
            name = {"", "en:"},
            info = {"", "en:"})
    public Boolean upload = false;

    @ModelField(
            show = "upload=false",
            name = {"", "en:"},
            info = {"", "en:"})
    public String path;

    @ModelField(
            show = "upload=true",
            type = FieldType.file,
            name = {"", "en:"},
            info = {"", "en:"})
    public String file;

    @Override
    public void start() {
        getAppContext().addI18n("app.exists", new String[]{"应用已存在，请更换为其它的应用名后重试",
                "en:If the application already exists, please change it to another application name and try again"});
        getAppContext().addI18n("app.not.found", new String[]{"应用文件未找到",
                "en:The app file was not found"});
        getAppContext().addI18n("app.type.unknown", new String[]{"应用文件类型无法识别",
                "en:The app file type is not recognized"});
    }

    @ModelAction(
            code = DeployerConstants.ACTION_INSTALL,
            name = {"安装应用", "en:Install App"},
            info = {"在该实例上安装应用。", "en:Install the application on the instance."})
    public void install(Request request) throws Exception {
        String appFile = Boolean.parseBoolean(request.getParameter("upload"))
                ? request.getParameter("file")
                : request.getParameter("path");
        File srcFile = new File(appFile);
        if (!srcFile.exists()) {
            request.getResponse().setSuccess(false);
            request.getResponse().setMsg(getAppContext().getI18n("app.not.found"));
            return;
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
            request.getResponse().setMsg(getAppContext().getI18n("app.type.unknown"));
            return;
        }

        Deployer deployer = Main.getService(Deployer.class);
        App deployerApp = deployer.getApp(app.getName());
        if (deployerApp != null) {
            request.getResponse().setSuccess(false);
            request.getResponse().setMsg(getAppContext().getI18n("app.exists"));
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
            info = {"代理上传文件操作。", "en:Proxy upload file operations."})
    public void upload(Request request) throws IOException {
        String fileId = request.getNonModelParameter(DeployerConstants.UPLOAD_FILE_ID);
        String fileName = request.getNonModelParameter(DeployerConstants.UPLOAD_FILE_NAME);
        byte[] fileBytes = Main.getService(CryptoService.class).getBase64Coder().decode(
                request.getNonModelParameter(DeployerConstants.UPLOAD_FILE_BYTES));

        File file = FileUtil.newFile(getAppContext().getTemp(), DeployerConstants.UPLOAD_FILE_TEMP_SUB_DIR, fileId, fileName);
        FileUtil.writeFile(file, fileBytes, true);
    }
}
