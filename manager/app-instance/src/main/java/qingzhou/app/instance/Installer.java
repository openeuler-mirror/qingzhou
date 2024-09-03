package qingzhou.app.instance;

import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelBase;
import qingzhou.api.Request;
import qingzhou.crypto.CryptoService;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.DeployerConstants;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.FileUtil;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

@Model(code = DeployerConstants.MODEL_INSTALLER,
        hidden = true,
        name = {"应用安装器", "en:App Installer"},
        info = {"执行管理节点下发的应用安装、卸载等指令。",
                "en:Execute the commands issued by the management node to install and uninstall applications."})
public class Installer extends ModelBase {
    @Override
    public void start() {
        appContext.addI18n("app.not.found", new String[]{"应用文件未找到", "en:File Not Found"});
        appContext.addI18n("file.upload.fail", new String[]{"文件上传失败", "en:File upload failed"});
    }

    @ModelAction(
            code = DeployerConstants.ACTION_INSTALL,
            name = {"安装应用", "en:Install App"},
            info = {"在该实例上安装应用。", "en:Install the application on the instance."})
    public void install(Request request) throws Exception {
        File srcFile;
        if (Boolean.parseBoolean(request.getParameter("upload"))) {
            srcFile = FileUtil.newFile(request.getParameter("file"));
        } else {
            srcFile = new File(request.getParameter(DeployerConstants.APP_KEY_PATH));
        }
        if (!srcFile.exists()) {
            request.getResponse().setSuccess(false);
            request.getResponse().setMsg(appContext.getI18n(request.getLang(), "app.not.found"));
            return;
        }

        String fileName = srcFile.getName();
        File app;
        String id = request.getParameter(DeployerConstants.APP_KEY_ID);
        if (srcFile.isDirectory()) {
            app = FileUtil.newFile(getAppsDir(), id == null ? fileName : id);
            FileUtil.copyFileOrDirectory(srcFile, app);
        } else if (fileName.endsWith(".jar")) {
            int index = fileName.lastIndexOf(".");
            String appName = fileName.substring(0, index);
            app = FileUtil.newFile(getAppsDir(), id == null ? appName : id);
            FileUtil.copyFileOrDirectory(srcFile, FileUtil.newFile(app, fileName));
        } else if (fileName.endsWith(".zip")) {
            int index = fileName.lastIndexOf(".");
            String appName = fileName.substring(0, index);
            app = FileUtil.newFile(getAppsDir(), id == null ? appName : id);
            FileUtil.unZipToDir(srcFile, app);
        } else {
            throw new IllegalArgumentException("unknown app type");
        }

        Main.getService(Deployer.class).installApp(app);
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
        String fileName = request.getParameter("fileName");
        String fileBytes = request.getParameter("fileBytes");
        boolean isStart = Boolean.parseBoolean(request.getParameter("isStart"));
        boolean isEnd = Boolean.parseBoolean(request.getParameter("isEnd"));
        int len = Integer.parseInt(request.getParameter("len"));
        String timestamp = request.getParameter("timestamp");
        File tempDir = FileUtil.newFile(Main.getInstanceDir(), "temp", request.getModel());
        File destFile = FileUtil.newFile(tempDir, timestamp, fileName);
        try {
            writeFile(destFile, Main.getService(CryptoService.class).getBase64Coder().decode(fileBytes), len, isStart);
        } catch (IOException e) {
            request.getResponse().setSuccess(false);
            request.getResponse().setMsg(appContext.getI18n(request.getLang(), "file.upload.fail"));
            FileUtil.forceDelete(destFile);
            return;
        }

        if (isEnd) {
            Map<String, String> data = new HashMap<>();
            data.put("fileName", destFile.getCanonicalPath());
            request.getResponse().addData(data);
        }
    }

    private void writeFile(File file, byte[] fileBytes, int len, boolean isStart) throws IOException {
        if (isStart) {
            if (file.exists()) {
                try {
                    FileUtil.forceDelete(file);
                } catch (Exception ignored) {
                }
            }
            FileUtil.mkdirs(file.getParentFile());
            file.createNewFile();
        }
        try (OutputStream out = new FileOutputStream(file, true); BufferedOutputStream bos = new BufferedOutputStream(out)) {
            bos.write(fileBytes, 0, len);
            bos.flush();
        } catch (IOException e) {
            FileUtil.forceDelete(file);
            throw e;
        }
    }
}
