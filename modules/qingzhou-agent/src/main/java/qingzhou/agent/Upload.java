package qingzhou.agent;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.common.FileUtil;
import qingzhou.common.TempFiles;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.logger.Logger;
import qingzhou.registry.AgentDomainKeys;
import qingzhou.registry.Constants;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component(configurationPid = "qingzhou-agent", configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {HttpHandler.HANDLE_PATH + "=" + qingzhou.registry.Constants.REMOTE_UPLOAD_URI, HttpHandler.HANDLE_NO_AUTH + "=true", HttpHandler.HANDLE_METHODS + "=POST"},
        service = {Upload.class, HttpHandler.class})
public class Upload extends AbstractAgentHandler {
    // 单上传总量上限：分片追加若无总量约束，攻击者可持续上传写满磁盘（DoS）
    private static final long MAX_UPLOAD_BYTES = 1024L * 1024 * 1024;

    // 上传目录整体兜底：单键上限挡不住"多个键各自合规"的累积占用
    private static final long TEMP_FILE_TTL_MILLIS = 24L * 60 * 60 * 1000;
    private static final int MAX_UPLOAD_FILES = 1000;
    private static final long MAX_UPLOAD_TOTAL_BYTES = 4L * 1024 * 1024 * 1024;
    // 一次上传独占一个目录：分片落在 data，业务落位时改名，同名文件不会互相覆盖
    private static final String DATA_FILE = "data";

    private File uploadBase;
    private String uploadBasePath; // 上传键边界校验的基准：canonical 绝对路径，避免每次请求重复解析

    // 聚合配额增量计数：写入路径 O(1) 判超限，无需逐片扫描目录；增删目录时同步增减
    private final AtomicInteger pendingDirs = new AtomicInteger();
    private final AtomicLong pendingBytes = new AtomicLong();

    @Reference
    public void setAgentDomainKeys(AgentDomainKeys agentDomainKeys) {
        this.agentDomainKeys = agentDomainKeys;
    }

    @Reference
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    @Activate
    public void init(Map<String, String> config) throws IOException {
        uploadBase = Paths.get(config.get("qingzhou.instance"), "temp", "agent-upload").toFile();
        FileUtil.mkdirs(uploadBase);
        uploadBasePath = uploadBase.getCanonicalPath();
        TempFiles.cleanExpired(uploadBase, TEMP_FILE_TTL_MILLIS); // 只清过期残留：配置刷新重激活不得删除在途上传
        recount(); // 清理后初始化配额计数，避免重启后计数虚低
    }

    @Override
    protected String aadName() {
        return qingzhou.registry.Constants.REMOTE_UPLOAD_URI;
    }

    @Override
    protected String[] aadContext(HttpRequest httpRequest) {
        // 端点名在前、文件键与偏移量在后：后两者是明文控制参数，纳入 AAD 后改道分片或改写偏移即解密失败
        return new String[]{aadName(), fileKey(httpRequest), fileOffset(httpRequest)};
    }

    @Override
    protected String doProcess(byte[] requestData, HttpRequest httpRequest) throws Throwable { // try catch 整体可以简化 todo
        long offset = Long.parseLong(fileOffset(httpRequest));
        String uploadId = fileKey(httpRequest);
        File dir = uploadId.isEmpty() ? null : uploadDir(uploadId);
        File data = dir == null ? null : dataFile(dir);
        // 偏移量受 AAD 保护，这里只比对落盘长度：不符即丢片或乱序；空键等价于长度 0
        if ((data == null ? 0 : data.length()) != offset) {
            throw new IllegalArgumentException("upload chunk out of order");
        }
        boolean newUpload = dir == null;
        if (newUpload) {
            cleanExpiredUploadDirs(); // 新建低频清理过期目录，回调同步配额计数
            if (pendingDirs.get() >= MAX_UPLOAD_FILES) {
                throw new IllegalArgumentException("too many pending uploads");
            }
            uploadId = UUID.randomUUID().toString();
            dir = createUploadDir(uploadId);
            data = dataFile(dir);
            pendingDirs.incrementAndGet();
        }
        // 原子预留本次分片字节：addAndGet 原子判超限，消除并发余量；新建已建目录，超限需回退目录计数并删除
        if (pendingBytes.addAndGet(requestData.length) > MAX_UPLOAD_TOTAL_BYTES) {
            pendingBytes.addAndGet(-requestData.length);
            if (newUpload) {
                pendingDirs.decrementAndGet();
                FileUtil.forceDeleteQuietly(dir);
            }
            throw new IllegalArgumentException("upload dir exceeds size limit");
        }

        try {
            // 超限由调用方数据量触发：按 IllegalArgumentException 归为 400，不计入服务端故障
            if (offset + requestData.length > MAX_UPLOAD_BYTES) {
                throw new IllegalArgumentException("upload exceeds size limit");
            }
            // 分片追加：O_APPEND 下每次写入对文件偏移原子，同键并发分片不会互相覆盖；创建即收紧为 0600
            try (OutputStream out = FileUtil.newOwnerOnlyFile(data.toPath(), StandardOpenOption.APPEND)) {
                out.write(requestData);
            }
        } catch (Throwable e) {
            // 中断即丢弃本次上传，避免残留半截文件；回退本次预留与目录已计入字节、目录数
            pendingBytes.addAndGet(-requestData.length - offset); // todo 审查此处的 - offset
            pendingDirs.decrementAndGet();
            FileUtil.forceDeleteQuietly(dir);
            throw e;
        }
        return uploadId;
    }

    /**
     * 上传键即目录名：以 canonical 包含关系界定边界，".."、"/"、"."、盘符等路径形态一并拒外，
     * 优于逐字符黑名单。只定位已有上传，不创建目录。
     */
    File uploadDir(String uploadId) {
        if (uploadId.isEmpty()) {
            throw new IllegalArgumentException("illegal upload id");
        }
        File dir = new File(uploadBase, uploadId);
        try {
            if (!dir.getCanonicalPath().startsWith(uploadBasePath + File.separator)) {
                throw new IllegalArgumentException("illegal upload id");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("illegal upload id", e);
        }
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("unknown upload id");
        }
        return dir;
    }

    File dataFile(File uploadDir) {
        return new File(uploadDir, DATA_FILE);
    }

    // 释放上传目录：扣减配额计数并删除（业务处理完调用）
    void release(File dir) {
        deductQuota(dir);
        FileUtil.forceDeleteQuietly(dir);
    }

    // 新建上传低频清理过期目录：TempFiles 内置节流，删除前经回调扣减配额计数
    private void cleanExpiredUploadDirs() {
        TempFiles.cleanExpired(uploadBase, TEMP_FILE_TTL_MILLIS, this::deductQuota);
    }

    // 目录配额计数扣减：删除前调用，保持计数与磁盘占用一致
    private void deductQuota(File dir) {
        pendingBytes.addAndGet(-sizeOf(dir));
        pendingDirs.decrementAndGet();
    }

    // 激活时全量重算计数，避免重启后配额计数虚低
    private void recount() {
        long total = 0;
        int dirs = 0;
        for (File dir : childrenOf(uploadBase)) {
            dirs++;
            total += sizeOf(dir);
        }
        pendingBytes.set(total);
        pendingDirs.set(dirs);
    }

    private static long sizeOf(File dir) {
        long total = 0;
        for (File child : childrenOf(dir)) {
            total += child.length();
        }
        return total;
    }

    private File createUploadDir(String uploadId) throws IOException {
        return FileUtil.createOwnerOnlyDir(new File(uploadBase, uploadId)); // 上传内容可能含敏感数据：目录收紧为 0700
    }

    private static File[] childrenOf(File dir) {
        File[] children = dir.listFiles();
        return children == null ? new File[0] : children;
    }

    private static String fileKey(HttpRequest httpRequest) {
        String key = httpRequest.getParameter(qingzhou.registry.Constants.REMOTE_PARAMETER_FILE_KEY);
        return key == null ? "" : key;
    }

    private static String fileOffset(HttpRequest httpRequest) {
        String offset = httpRequest.getParameter(Constants.REMOTE_PARAMETER_FILE_OFFSET);
        if (offset == null) {
            throw new IllegalArgumentException("missing upload offset");
        }
        return offset;
    }
}
