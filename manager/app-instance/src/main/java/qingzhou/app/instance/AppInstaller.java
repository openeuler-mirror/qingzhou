package qingzhou.app.instance;

import qingzhou.api.*;
import qingzhou.crypto.CryptoService;
import qingzhou.deployer.Deployer;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.FileUtil;

import java.io.*;
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
            srcFile = FileUtil.newFile(request.getParameter("fromUpload"));
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
            app = FileUtil.newFile(getAppsDir(), id == null ? srcFileName : id);
            FileUtil.copyFileOrDirectory(srcFile, app);
        } else if (srcFileName.endsWith(".jar")) {
            int index = srcFileName.lastIndexOf(".");
            String appName = srcFileName.substring(0, index);
            app = FileUtil.newFile(getAppsDir(), id == null ? appName : id);
            FileUtil.copyFileOrDirectory(srcFile, FileUtil.newFile(app, srcFileName));
        } else if (srcFileName.endsWith(".zip")) {
            int index = srcFileName.lastIndexOf(".");
            String appName = srcFileName.substring(0, index);
            app = FileUtil.newFile(getAppsDir(), id == null ? appName : id);
            FileUtil.unZipToDir(srcFile, app);
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
        FileUtil.forceDelete(FileUtil.newFile(getAppsDir(), request.getId()));
    }

    private File getAppsDir() {
        return FileUtil.newFile(InstanceApp.getService(ModuleContext.class).getInstanceDir(), "apps");
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
        File tempDir = FileUtil.newFile(InstanceApp.getInstanceDir(), "temp", request.getModel());
        File destFile = FileUtil.newFile(tempDir, timestamp, fileName);
        try {
            writeFile(destFile, appContext.getService(CryptoService.class).getHexCoder().hexToBytes(fileBytes), len, isStart);
        } catch (IOException e) {
            response.setSuccess(false);
            response.setMsg(appContext.getI18n(request.getLang(), "file.upload.fail"));
            FileUtil.forceDelete(destFile);
            return;
        }

        if (isEnd) {
            Map<String, String> data = new HashMap<>();
            data.put("fileName", destFile.getCanonicalPath());
            response.addData(data);
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
