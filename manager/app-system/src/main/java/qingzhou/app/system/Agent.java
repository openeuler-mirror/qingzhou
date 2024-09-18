package qingzhou.app.system;

import qingzhou.api.*;
import qingzhou.api.type.Addable;
import qingzhou.api.type.Deletable;
import qingzhou.api.type.Downloadable;
import qingzhou.api.type.Monitorable;
import qingzhou.crypto.CryptoService;
import qingzhou.deployer.App;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.DeployerConstants;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.lang.management.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Model(code = DeployerConstants.MODEL_AGENT,
        hidden = true,
        name = {"实例代理", "en:Agent"},
        info = {"", "en:"})
public class Agent extends ModelBase implements Downloadable {
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
            code = Addable.ACTION_ADD,
            name = {"", "en:"},
            info = {"", "en:"})
    public void installApp(Request request) throws Exception {
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
            code = Deletable.ACTION_DELETE,
            name = {"", "en:"},
            info = {"", "en:"})
    public void uninstallApp(Request request) throws Exception {
        Main.getService(Deployer.class).unInstallApp(request.getId());
        FileUtil.forceDelete(FileUtil.newFile(getAppsDir(), request.getId()));
    }

    private File getAppsDir() {
        return FileUtil.newFile(Main.getService(ModuleContext.class).getInstanceDir(), "apps");
    }

    @Override
    public File downloadData(String id) {
        return new File(Main.getService(ModuleContext.class).getInstanceDir(), "logs");
    }

    @ModelAction(
            code = Monitorable.ACTION_MONITOR,
            name = {"", "en:"},
            info = {"", "en:"})
    public void monitor(Request request) {
        OperatingSystemMXBean mxBean = ManagementFactory.getOperatingSystemMXBean();

        Map<String, String> data = new HashMap<>();
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

        request.getResponse().addData(data);
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
            name = {"", "en:"},
            info = {"", "en:"})
    public void uploadTempFile(Request request) throws IOException {
        String fileId = request.getNonModelParameter(DeployerConstants.UPLOAD_FILE_ID);
        String fileName = request.getNonModelParameter(DeployerConstants.UPLOAD_FILE_NAME);
        byte[] fileBytes = Main.getService(CryptoService.class).getBase64Coder().decode(
                request.getNonModelParameter(DeployerConstants.UPLOAD_FILE_BYTES));

        File file = FileUtil.newFile(getAppContext().getTemp(), DeployerConstants.UPLOAD_FILE_TEMP_SUB_DIR, fileId, fileName);
        FileUtil.writeFile(file, fileBytes, true);
    }
}
