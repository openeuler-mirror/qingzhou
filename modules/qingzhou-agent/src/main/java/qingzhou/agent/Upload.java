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
import qingzhou.common.TempFiles;
import qingzhou.crypto.Crypto;
import qingzhou.dto.Constants;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.logger.Logger;
import qingzhou.registry.AgentDomainKeys;

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

    private File uploadBase;

    @Activate
    public void init(Map<String, String> config) {
        uploadBase = Paths.get(config.get("qingzhou.instance"), "temp", "agent-upload").toFile();
        if (!uploadBase.mkdirs()) {
            logger.error("failed to mkdirs: " + uploadBase.getAbsolutePath());
        }
        TempFiles.cleanDir(uploadBase); // 清历史残留
    }

    @Override
    protected String aadName() {
        return Constants.REMOTE_UPLOAD_URI;
    }

    @Override
    protected String doProcess(byte[] requestData, HttpRequest httpRequest) throws Throwable {
        String key = httpRequest.getParameter(Constants.REMOTE_PARAMETER_FILE_KEY);
        String uploadId = key != null && !key.isEmpty() ? key : UUID.randomUUID().toString();
        File tempFile = resolveInUploadBase(uploadId);
        if (!tempFile.exists()) {
            enforceUploadDirQuota(); // 仅在新建上传时扫描目录：分片追加不重复付出目录遍历开销
        }
        try {
            long total = tempFile.length() + requestData.length;
            if (total > MAX_UPLOAD_BYTES) { // 超限即终止并清理，避免磁盘被写满
                throw new IllegalStateException("upload exceeds size limit: " + uploadId);
            }
            Files.write(tempFile.toPath(), requestData, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            // 追加后复核：写入前检查与 APPEND 非原子（TOCTOU），并发分片可越过上限
            if (tempFile.length() > MAX_UPLOAD_BYTES) {
                throw new IllegalStateException("upload exceeds size limit: " + uploadId);
            }
        } catch (Throwable e) {
            if (!tempFile.delete()) {
                logger.error("failed to delete upload file: " + tempFile.getAbsolutePath());
            }
            throw e;
        }

        return uploadId;
    }

    // 上传目录容量兜底：文件数与累积体积双限，超出即拒绝新建上传（已过 TTL 的残留由 cleanExpired 回收）
    private void enforceUploadDirQuota() {
        TempFiles.cleanExpired(uploadBase, TEMP_FILE_TTL_MILLIS);

        File[] files = uploadBase.listFiles();
        if (files == null) return;
        if (files.length >= MAX_UPLOAD_FILES) {
            throw new IllegalStateException("too many pending uploads: " + files.length);
        }
        long total = 0;
        for (File file : files) {
            total += file.length();
        }
        if (total >= MAX_UPLOAD_TOTAL_BYTES) {
            throw new IllegalStateException("upload dir exceeds size limit: " + total);
        }
    }

    // 参数中的路径来自不可信请求，必须限制在上传目录内，否则可越界写入/删除任意文件；
    // 路径可能是相对名（上传 id），也可能是应用回传的绝对路径（须落在上传目录内）
    File resolveInUploadBase(String path) {
        try {
            File file = new File(path);
            if (!file.isAbsolute()) file = new File(uploadBase, path);
            if (file.getCanonicalFile().getPath().startsWith(uploadBase.getCanonicalPath() + File.separator)) {
                return file;
            }
        } catch (IOException e) {
            // 路径非法
        }
        throw new IllegalArgumentException("illegal file path: " + path);
    }
}
