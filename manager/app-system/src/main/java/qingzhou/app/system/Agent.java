package qingzhou.app.system;

import qingzhou.api.*;
import qingzhou.api.type.Download;
import qingzhou.api.type.Monitor;
import qingzhou.deployer.App;
import qingzhou.deployer.*;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.lang.management.*;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Model(code = DeployerConstants.MODEL_AGENT,
        hidden = true,
        name = {"实例代理", "en:Agent"})
public class Agent extends ModelBase implements Download {
    @ModelField(
            input_type = InputType.file, // ActionInvokerImpl.invokeOnInstances 中调用  getFileUploadFieldNames，依赖这个 file 类型
            name = {"", "en:"})
    public String file; // ActionInvokerImpl.invokeOnInstances 中调用  getFileUploadFieldNames，依赖这个 file 类型

    @Override
    public void start() {
        getAppContext().addI18n("file.exists", new String[]{"文件已存在，请更换为其它的文件后重试",
                "en:The file already exists, please replace it with another file and try again"});
        getAppContext().addI18n("file.not.found", new String[]{"文件未找到", "en:File not found"});
        getAppContext().addI18n("file.type.unknown", new String[]{"文件类型无法识别", "en:The file type is not recognized"});
    }

    @ModelAction(
            code = DeployerConstants.ACTION_INSTALL_APP,
            name = {"", "en:"})
    public void installApp(Request request) throws Exception {
        String appFile = Boolean.parseBoolean(request.getParameter("upload"))
                ? request.getParameter("file")
                : request.getParameter("path");
        File srcFile = new File(appFile);
        if (!srcFile.exists()) {
            request.getResponse().setSuccess(false);
            request.getResponse().setMsg(getAppContext().getI18n("file.not.found"));
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
            request.getResponse().setMsg(getAppContext().getI18n("file.type.unknown"));
            return;
        }

        Deployer deployer = Main.getService(Deployer.class);
        App deployerApp = deployer.getApp(app.getName());
        if (deployerApp != null) {
            request.getResponse().setSuccess(false);
            request.getResponse().setMsg(getAppContext().getI18n("file.exists"));
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
            code = DeployerConstants.ACTION_UNINSTALL_APP,
            name = {"", "en:"})
    public void uninstallApp(Request request) throws Exception {
        Main.getService(Deployer.class).unInstallApp(request.getId());
        FileUtil.forceDelete(FileUtil.newFile(getAppsDir(), request.getId()));
    }

    @ModelAction(
            code = DeployerConstants.ACTION_DOWNLOAD_PAGE,
            name = {"", "en:"})
    public void downloadPage(Request request) throws IOException {
        String app = request.getParameter(DeployerConstants.DOWNLOAD_PAGE_APP);
        String dir = request.getParameter(DeployerConstants.DOWNLOAD_PAGE_DIR);
        File appPageDir = FileUtil.newFile(getAppsDir(), app, dir);
        if (appPageDir.exists()) {
            File zipFile = FileUtil.newFile(Main.getService(ModuleContext.class).getTemp(), dir + ".zip");
            try {
                FileUtil.zipFiles(appPageDir, zipFile, true);
                ResponseImpl response = (ResponseImpl) request.getResponse();
                response.setBodyBytes(Files.readAllBytes(zipFile.toPath()));
            } finally {
                FileUtil.forceDelete(zipFile);
            }
        }
    }

    private File getAppsDir() {
        return FileUtil.newFile(Main.getService(ModuleContext.class).getInstanceDir(), "apps");
    }

    @Override
    public File downloadData(String id) {
        return new File(Main.getService(ModuleContext.class).getInstanceDir(), "logs");
    }

    @ModelAction(
            code = Monitor.ACTION_MONITOR,
            name = {"", "en:"})
    public void monitor(Request request) {
        OperatingSystemMXBean mxBean = ManagementFactory.getOperatingSystemMXBean();

        LinkedHashMap<String, String> data = new LinkedHashMap<>();
        data.put("osName", mxBean.getName());
        data.put("osVer", mxBean.getVersion());
        data.put("arch", mxBean.getArch());
        data.put("cpu", String.valueOf(mxBean.getAvailableProcessors()));

        long totalSpace = 0;
        long usableSpace = 0;
        for (File file : File.listRoots()) { // 所有磁盘计算总和
            totalSpace += file.getTotalSpace();
            usableSpace += file.getUsableSpace();
        }
        data.put("disk", maskGBytes(totalSpace));
        data.put("diskUsed", maskGBytes(totalSpace - usableSpace));

        double v = mxBean.getSystemLoadAverage() / mxBean.getAvailableProcessors();// mac 等系统
        data.put("cpuUsed", String.format("%.2f", v));

        data.putAll(jvmBasic());
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        int threadCount = threadMXBean.getThreadCount();
        long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
        data.put("threadCount", String.valueOf(threadCount));
        int deadlockedThreadCount = 0;
        if (deadlockedThreads != null) {
            deadlockedThreadCount = deadlockedThreads.length;
        }
        data.put("deadlockedThreadCount", String.valueOf(deadlockedThreadCount));

        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        data.put("heapUsed", maskMBytes(memoryMXBean.getHeapMemoryUsage().getUsed()));
        data.put("heapCommitted", maskMBytes(memoryMXBean.getHeapMemoryUsage().getCommitted()));
        data.put("nonHeapUsed", maskMBytes(memoryMXBean.getNonHeapMemoryUsage().getUsed()));

        data.forEach((key, value) -> request.getResponse().addDataMap(key, value));
    }

    private String maskGBytes(long val) {
        double v = ((double) val) / 1024 / 1024 / 1024;
        DecimalFormat df = new DecimalFormat("##0.0");//这样为保持1位
        return df.format(v);
    }

    private String maskMBytes(long val) {
        double v = ((double) val) / 1024 / 1024;
        DecimalFormat df = new DecimalFormat("##0.0");//这样为保持1位
        return df.format(v);
    }

    private static Map<String, String> jvmBasic;

    private Map<String, String> jvmBasic() {
        if (jvmBasic != null) return jvmBasic;

        Map<String, String> data = new HashMap<>();
        RuntimeMXBean mxBean = ManagementFactory.getRuntimeMXBean();
        data.put("specName", mxBean.getSpecName());
        data.put("specVersion", mxBean.getSpecVersion());
        data.put("vmName", mxBean.getVmName());
        data.put("vmVendor", mxBean.getVmVendor());
        data.put("vmVersion", mxBean.getVmVersion());
        data.put("Name", mxBean.getName());

        String format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(mxBean.getStartTime()));
        data.put("startTime", format);

        jvmBasic = data;
        return jvmBasic;
    }

    @ModelAction(
            code = DeployerConstants.ACTION_UPLOAD,
            name = {"", "en:"})
    public void uploadTempFile(Request request) throws IOException {
        String fileId = request.getParameter(DeployerConstants.UPLOAD_FILE_ID);
        String fileName = request.getParameter(DeployerConstants.UPLOAD_FILE_NAME);
        String appName = request.getParameter(DeployerConstants.UPLOAD_APP_NAME);
        byte[] fileBytes = ((RequestImpl) request).getByteParameter();
        App app = Main.getService(Deployer.class).getApp(appName);
        File file = FileUtil.newFile(app.getAppContext().getTemp(), DeployerConstants.UPLOAD_FILE_TEMP_SUB_DIR, fileId, fileName);
        FileUtil.writeFile(file, fileBytes, true);
    }

    @ModelAction(
            code = DeployerConstants.ACTION_INSTALL_VERSION,
            name = {"", "en:"})
    public void installVersion(Request request) throws Exception {
        String appFile = Boolean.parseBoolean(request.getParameter("upload"))
                ? request.getParameter("file")
                : request.getParameter("path");
        File srcFile = new File(appFile);
        if (!srcFile.exists()) {
            request.getResponse().setSuccess(false);
            request.getResponse().setMsg(getAppContext().getI18n("file.not.found"));
            return;
        }

        String fileName = srcFile.getName();
        if (!fileName.startsWith(Main.QZ_VER_NAME)) {
            request.getResponse().setSuccess(false);
            request.getResponse().setMsg(getAppContext().getI18n("file.type.unknown"));
            return;
        }

        File targetFile = new File(Main.getLibBase(), fileName);
        if (targetFile.exists()) {
            return;
        }

        FileUtil.copyFileOrDirectory(srcFile, targetFile);
    }

    @ModelAction(
            code = DeployerConstants.ACTION_UNINSTALL_VERSION,
            name = {"", "en:"})
    public void deleteVersion(Request request) throws Exception {
        String version = request.getId();
        if (version.equals(getAppContext().getPlatformVersion())) return;

        FileUtil.forceDelete(new File(Main.getLibBase(), Main.QZ_VER_NAME + version));
        FileUtil.forceDelete(new File(Main.getLibBase(), Main.QZ_VER_NAME + version + ".zip"));
    }

    @ModelAction(
            code = DeployerConstants.ACTION_START_APP,
            name = {"", "en:"})
    public void startApp(Request request) throws Exception {
        String name = request.getId();
        Deployer deployer = Main.getService(Deployer.class);
        deployer.startApp(name);
    }

    @ModelAction(
            code = DeployerConstants.ACTION_STOP_APP,
            name = {"", "en:"})
    public void stopApp(Request request) throws Exception {
        Deployer deployer = Main.getService(Deployer.class);
        deployer.stopApp(request.getId());
    }
}
