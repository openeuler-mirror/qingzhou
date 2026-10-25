package qingzhou.agent;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.common.FileUtil;
import qingzhou.common.TempFiles;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.logger.Logger;
import qingzhou.registry.AgentDomainService;
import qingzhou.registry.Constants;

@Component(configurationPid = "qingzhou-agent", configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {HttpHandler.HANDLE_PATH + "=" + Constants.REMOTE_UPLOAD_URI, HttpHandler.HANDLE_NO_AUTH + "=true", HttpHandler.HANDLE_METHODS + "=POST"},
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
    public void setAgentDomainKeys(AgentDomainService agentDomainService) {
        this.agentDomainService = agentDomainService;
    }

    @Reference
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    @Reference
    public void setAgentCipherManager(AgentCipherManager agentCipherManager) {
        this.agentCipherManager = agentCipherManager;
    }

    @Reference
    public void setAgentInstanceManager(AgentInstanceManager agentInstanceManager) {
        this.agentInstanceManager = agentInstanceManager;
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
    protected String uriName() {
        return Constants.REMOTE_UPLOAD_URI;
    }

    @Override
    protected String[] aadContext(HttpRequest httpRequest) {
        // 端点名在前、文件键与偏移量在后：后两者是明文控制参数，纳入 AAD 后改道分片或改写偏移即解密失败
        return new String[]{fileKey(httpRequest), fileOffset(httpRequest)};
    }

    @Override
    protected String doProcess(byte[] requestData, HttpRequest httpRequest) throws Throwable {
        // 参数合法性检查
        long offset = Long.parseLong(fileOffset(httpRequest));
        String uploadId = fileKey(httpRequest);
        File dir = uploadId.isEmpty() ? null : uploadDir(uploadId);
        File data = dir == null ? null : dataFile(dir);
        // 参数合法性：偏移量受 AAD 保护，须与落盘长度一致，否则丢片或乱序；空键等价于长度 0
        if ((data == null ? 0 : data.length()) != offset) {
            throw new IllegalArgumentException("upload chunk out of order");
        }

        boolean newUpload = dir == null;
        if (newUpload) {
            cleanExpiredUploadDirs(); // 新建低频清理过期目录，回调同步配额计数
            uploadId = UUID.randomUUID().toString();
            dir = createUploadDir(uploadId);
            data = dataFile(dir);
        }

        // 配额预留：目录数/单文件/聚合总量原子判超限，失败即回退并删目录；句柄在写入失败时经 try-with-resources 自动回退
        try (QuotaReservation reservation = reserveQuota(requestData.length, offset, newUpload, dir)) {
            // 分片追加：O_APPEND 下每次写入对文件偏移原子，同键并发分片不会互相覆盖；创建即收紧为 0600
            try (OutputStream out = FileUtil.newOwnerOnlyFile(data.toPath(), StandardOpenOption.APPEND)) {
                out.write(requestData);
            }
            reservation.commit();
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

    // 配额预留：目录数、单文件、聚合总量三项原子判超限，失败即回退并删目录；成功返回可自动回退的句柄
    private QuotaReservation reserveQuota(long len, long offset, boolean newUpload, File dir) {
        if (newUpload && pendingDirs.incrementAndGet() > MAX_UPLOAD_FILES) {
            rollback(0, dir);
            throw new IllegalArgumentException("too many pending uploads");
        }
        if (offset + len > MAX_UPLOAD_BYTES) {
            rollback(offset, dir);
            throw new IllegalArgumentException("upload exceeds size limit");
        }
        if (pendingBytes.addAndGet(len) > MAX_UPLOAD_TOTAL_BYTES) {
            rollback(len + offset, dir);
            throw new IllegalArgumentException("upload dir exceeds size limit");
        }
        return new QuotaReservation(len + offset, dir);
    }

    // 回退配额并删除目录：删除路径统一入口，保证计数与磁盘占用一致
    private void rollback(long bytes, File dir) {
        pendingBytes.addAndGet(-bytes);
        pendingDirs.decrementAndGet();
        FileUtil.forceDeleteQuietly(dir);
    }

    // 配额预留句柄：未 commit 即回退预留并删除目录（try-with-resources 自动调用 close）
    private final class QuotaReservation implements AutoCloseable {
        private final long bytes;
        private final File dir;
        private boolean committed;

        QuotaReservation(long bytes, File dir) {
            this.bytes = bytes;
            this.dir = dir;
        }

        void commit() {
            committed = true;
        }

        @Override
        public void close() {
            if (!committed) rollback(bytes, dir);
        }
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
        String key = httpRequest.getParameter(Constants.REMOTE_PARAMETER_FILE_KEY);
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
