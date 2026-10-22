package qingzhou.agent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.UUID;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.common.FileUtil;
import qingzhou.common.TempFiles;
import qingzhou.crypto.Crypto;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.logger.Logger;
import qingzhou.registry.AgentDomainKeys;
import qingzhou.registry.Constants;

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

    @Reference
    public void setAgentDomainKeys(AgentDomainKeys agentDomainKeys) {
        this.agentDomainKeys = agentDomainKeys;
    }

    @Reference
    public void setCrypto(Crypto crypto) {
        this.crypto = crypto;
    }

    @Reference
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    @Activate
    public void init(Map<String, String> config) throws IOException {
        uploadBase = Paths.get(config.get("qingzhou.instance"), "temp", "agent-upload").toFile();
        FileUtil.mkdirs(uploadBase);
        TempFiles.cleanExpired(uploadBase, TEMP_FILE_TTL_MILLIS); // 只清过期残留：配置刷新重激活不得删除在途上传
    }

    @Override
    protected String aadName() {
        return qingzhou.registry.Constants.REMOTE_UPLOAD_URI;
    }

    @Override
    protected String[] aadContext(HttpRequest httpRequest) {
        // 端点名在前、文件键与偏移量在后：后两者是明文控制参数，纳入 AAD 后改道分片或乱序发送都会解密失败
        return new String[]{aadName(), fileKey(httpRequest), fileOffset(httpRequest)};
    }

    @Override
    protected String doProcess(byte[] requestData, HttpRequest httpRequest) throws Throwable {
        long offset = Long.parseLong(fileOffset(httpRequest));
        String uploadId = fileKey(httpRequest);
        File dir;
        if (uploadId.isEmpty()) {
            enforceUploadDirQuota(requestData.length); // 先查配额再建目录：被拒绝时不得继续产生空目录
            uploadId = UUID.randomUUID().toString();
            dir = createUploadDir(uploadId);
        } else {
            dir = uploadDir(uploadId);
        }

        File data = dataFile(dir);
        // 偏移量由发送方给出并受 AAD 保护：与已落盘长度不符即丢片或乱序
        if (data.length() != offset) {
            throw new IllegalArgumentException("upload chunk out of order");
        }

        try {
            if (offset + requestData.length > MAX_UPLOAD_BYTES) {
                throw new IllegalStateException("upload exceeds size limit: " + uploadId);
            }
            Files.write(data.toPath(), requestData, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable e) {
            // 中断即丢弃本次上传，避免残留半截文件
            FileUtil.forceDeleteQuietly(dir);
            throw e;
        }
        return uploadId;
    }

    /**
     * 上传键即目录名：只接受单段名字，不接受任何路径形式，杜绝越界写入。
     * 只定位已有上传，不创建目录。
     */
    File uploadDir(String uploadId) {
        if (uploadId.isEmpty() || uploadId.contains("..") || uploadId.indexOf('/') >= 0
                || uploadId.indexOf('\\') >= 0 || uploadId.indexOf(':') >= 0) {
            throw new IllegalArgumentException("illegal upload id: " + uploadId);
        }
        File dir = new File(uploadBase, uploadId);
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("unknown upload id: " + uploadId);
        }
        return dir;
    }

    File dataFile(File uploadDir) {
        return new File(uploadDir, DATA_FILE);
    }

    // 上传目录容量兜底：文件数与累积体积双限，超出即拒绝新建上传（过期残留由 cleanExpired 回收）
    private void enforceUploadDirQuota(long incomingBytes) {
        TempFiles.cleanExpired(uploadBase, TEMP_FILE_TTL_MILLIS);

        File[] dirs = uploadBase.listFiles();
        if (dirs == null) return;
        if (dirs.length >= MAX_UPLOAD_FILES) {
            throw new IllegalStateException("too many pending uploads: " + dirs.length);
        }
        long total = incomingBytes; // 计入本次待写分片，避免"写前总量"始终低于上限
        for (File dir : dirs) {
            for (File child : childrenOf(dir)) {
                total += child.length();
            }
        }
        if (total >= MAX_UPLOAD_TOTAL_BYTES) {
            throw new IllegalStateException("upload dir exceeds size limit: " + total);
        }
    }

    private File createUploadDir(String uploadId) {
        File dir = new File(uploadBase, uploadId);
        if (!dir.mkdirs()) {
            throw new IllegalStateException("failed to create upload dir: " + dir.getAbsolutePath());
        }
        return dir;
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
