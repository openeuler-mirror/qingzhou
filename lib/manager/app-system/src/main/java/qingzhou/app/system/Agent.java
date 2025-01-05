package qingzhou.app.system;

import java.io.File;
import java.io.IOException;
import java.lang.management.*;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import qingzhou.api.*;
import qingzhou.api.type.Download;
import qingzhou.api.type.Monitor;
import qingzhou.app.system.business.App;
import qingzhou.core.AppPageData;
import qingzhou.core.DeployerConstants;
import qingzhou.core.deployer.AppManager;
import qingzhou.core.deployer.Deployer;
import qingzhou.core.deployer.RequestImpl;
import qingzhou.core.deployer.ResponseImpl;
import qingzhou.core.registry.AppInfo;
import qingzhou.core.registry.AppState;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.Utils;

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
    public void installApp(Request request) throws Throwable {
        String appFile = Boolean.parseBoolean(request.getParameter("upload"))
                ? request.getParameter("file")
                : request.getParameter("path");
        File srcFile = new File(appFile);
        if (!srcFile.exists()) {
            request.getResponse().setSuccess(false);
            request.getResponse().setMsg(getAppContext().getI18n("file.not.found"));
            return;
        }

        File app;
        String fileName = srcFile.getName();
        if (srcFile.isDirectory()) {
            app = FileUtil.newFile(getAppsDir(), fileName);
            FileUtil.copyFileOrDirectory(srcFile, app);
        } else {
            int index = fileName.lastIndexOf(".");
            String appName = fileName.substring(0, index);
            app = FileUtil.newFile(getAppsDir(), appName);
            if (fileName.endsWith(".jar")) {
                FileUtil.copyFileOrDirectory(srcFile, FileUtil.newFile(app, fileName));
            } else if (fileName.endsWith(".zip")) {
                FileUtil.unZipToDir(srcFile, app);
            }
        }
        if (!app.isDirectory()) {
            request.getResponse().setSuccess(false);
            request.getResponse().setMsg(getAppContext().getI18n("file.type.unknown"));
            return;
        }

        Deployer deployer = Main.getService(Deployer.class);
        AppManager deployerAppManager = deployer.getApp(app.getName());
        if (deployerAppManager != null) {
            request.getResponse().setSuccess(false);
            request.getResponse().setMsg(getAppContext().getI18n("file.exists"));
            return;
        }

        String appInstalled;
        try {
            Properties properties = getDeploymentProperties(request);
            appInstalled = deployer.installApp(app, properties);
        } catch (Throwable e) {
            FileUtil.forceDelete(app);
            throw e;
        }

        deployer.startApp(appInstalled);
    }

    private Properties getDeploymentProperties(Request request) {
        Properties properties = null;
        String deploymentProperties = request.getParameter(DeployerConstants.DEPLOYMENT_PROPERTIES);
        if (Utils.notBlank(deploymentProperties)) {
            properties = new Properties();
            String[] split = deploymentProperties.split(App.DEPLOYMENT_PROPERTIES_SP);
            for (String s : split) {
                int i = s.indexOf("=");
                if (i > 0) {
                    properties.setProperty(s.substring(0, i), s.substring(i + 1));
                }
            }
        }
        return properties;
    }

    @ModelAction(
            code = DeployerConstants.ACTION_UNINSTALL_APP,
            name = {"", "en:"})
    public void uninstallApp(Request request) throws Exception {
        Deployer deployer = Main.getService(Deployer.class);
        deployer.stopApp(request.getId());
        deployer.unInstallApp(request.getId());
        FileUtil.forceDelete(FileUtil.newFile(getAppsDir(), request.getId()));
    }

    @ModelAction(
            code = AppPageData.ACTION_DOWNLOAD_PAGE,
            name = {"", "en:"})
    public void downloadPage(Request request) throws IOException {
        String app = request.getParameter(AppPageData.DOWNLOAD_PAGE_APP);
        String dir = request.getParameter(AppPageData.DOWNLOAD_PAGE_DIR);
        File appPageDir = FileUtil.newFile(getAppsDir(), app, dir);
        if (appPageDir.exists()) {
            File zipFile = FileUtil.newFile(Main.getService(ModuleContext.class).getTemp(), dir + ".zip");
            try {
                FileUtil.zipFiles(appPageDir, zipFile, true);
                ResponseImpl response = (ResponseImpl) request.getResponse();
                response.setInternalData(Files.readAllBytes(zipFile.toPath()));
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

        HashMap<String, String> data = new HashMap<>();
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

        ResponseImpl response = (ResponseImpl) request.getResponse();
        response.setInternalData(data);
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
        AppManager appManager = Main.getService(Deployer.class).getApp(appName);
        File file = FileUtil.newFile(appManager.getAppContext().getTemp(), DeployerConstants.UPLOAD_FILE_TEMP_SUB_DIR, fileId, fileName);
        FileUtil.writeFile(file, fileBytes, true);
    }

    @ModelAction(
            code = DeployerConstants.ACTION_INSTALL_VERSION,
            name = {"", "en:"})
    public void installVersion(Request request) throws Exception {
        File srcFile = getUploadedFile(request);
        if (srcFile == null) return;

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

    private File getUploadedFile(Request request) {
        String appFile = Boolean.parseBoolean(request.getParameter("upload"))
                ? request.getParameter("file")
                : request.getParameter("path");
        File srcFile = new File(appFile);
        if (!srcFile.exists()) {
            request.getResponse().setSuccess(false);
            request.getResponse().setMsg(getAppContext().getI18n("file.not.found"));
            return null;
        }

        return srcFile;
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
    public void startApp(Request request) throws Throwable {
        Deployer deployer = Main.getService(Deployer.class);
        deployer.startApp(request.getId());
    }

    @ModelAction(
            code = DeployerConstants.ACTION_STOP_APP,
            name = {"", "en:"})
    public void stopApp(Request request) {
        Deployer deployer = Main.getService(Deployer.class);
        deployer.stopApp(request.getId());
    }

    @ModelAction(
            code = DeployerConstants.ACTION_UPDATE_APP,
            name = {"", "en:"})
    public void updateApp(Request request) throws Throwable {
        String appName = request.getId();
        Deployer deployer = Main.getService(Deployer.class);
        AppInfo appInfo = deployer.getAppInfo(appName);
        Properties properties = getDeploymentProperties(request);
        if (properties != null && !properties.isEmpty()) {
            String appDir = appInfo.getFilePath();
            File preferablyFile = new File(appDir, DeployerConstants.QINGZHOU_PROPERTIES_FILE);
            FileUtil.writeFile(preferablyFile, properties);
        }

        boolean isRunning = appInfo.getState() == AppState.Started;
        if (isRunning) {
            deployer.stopApp(appName);
            deployer.startApp(appName);
        }
    }

    @ModelAction(
            code = DeployerConstants.ACTION_GC,
            name = {"", "en:"})
    public void gc(Request request) {
        System.gc();
    }
}
