package qingzhou.app.instance;

import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelBase;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.deployer.Deployer;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.crypto.CryptoServiceFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Model(code = "appinstaller",
        hidden = true,
        name = {"应用安装器", "en:App Installer"},
        info = {"执行管理节点下发的应用安装、卸载等指令。",
                "en:Execute the commands issued by the management node to install and uninstall applications."})
public class AppInstaller extends ModelBase {
    @Override
    public void start() {
        appContext.addI18n("app.not.found", new String[]{"应用文件未找到", "en:File Not Found"});
        appContext.addI18n("file.upload.fail", new String[]{"文件上传失败", "en:File upload failed"});
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

    @ModelAction(
            name = {"上传文件", "en:Upload File"},
            info = {"应用模块表单文件上传。", "en:Upload the application module form file."})
    public void uploadFile(Request request, Response response) throws IOException {
        String fileName = request.getParameter("fileName");
        String fileBytes = request.getParameter("fileBytes");
        boolean isStart = Boolean.parseBoolean(request.getParameter("isStart"));
        boolean isEnd = Boolean.parseBoolean(request.getParameter("isEnd"));
        int len = Integer.parseInt(request.getParameter("len"));
        String timestamp = request.getParameter("timestamp");
        File tempDir = Utils.newFile(InstanceApp.getInstanceDir(), "temp", request.getModel());
        File destFile = Utils.newFile(tempDir, timestamp, fileName);
        try {
            Utils.writeFile(destFile, CryptoServiceFactory.getInstance().getMessageDigest().hexToBytes(fileBytes), len, isStart);
        } catch (IOException e) {
            response.setSuccess(false);
            response.setMsg(appContext.getI18n(request.getLang(), "file.upload.fail"));
            Utils.forceDelete(destFile);
            return;
        }

        if (isEnd) {
            Map<String, String> data = new HashMap<>();
            data.put("fileName", destFile.getCanonicalPath());
            response.addData(data);
        }
    }
}
