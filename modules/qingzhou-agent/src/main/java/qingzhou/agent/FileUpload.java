package qingzhou.agent;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.UUID;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.crypto.Crypto;
import qingzhou.dto.Constants;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.logger.Logger;

@Component(configurationPid = "qingzhou-agent", configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {HttpHandler.HANDLE_PATH + "=" + Constants.AGENT_UPLOAD_URI, HttpHandler.HANDLE_NO_AUTH + "=true", HttpHandler.HANDLE_METHODS + "=POST"},
        service = {FileUpload.class, HttpHandler.class})
public class FileUpload implements HttpHandler {
    @Reference
    private Crypto crypto;
    @Reference
    private Logger logger;

    private File uploadBase;

    @Activate
    public void init(Map<String, String> config) {
        uploadBase = Paths.get(config.get("qingzhou.instance"), "temp", "agent-upload").toFile();
        uploadBase.mkdirs();
        cleanLegacyUploads();
    }

    private void cleanLegacyUploads() {
        File[] legacy = uploadBase.listFiles();
        if (legacy == null) return;
        for (File file : legacy) {
            file.delete();
        }
    }

    private void cleanExpiredUploads() {
        File[] files = uploadBase.listFiles();
        if (files == null) return;

        // 运行期惰性清理：分片上传中断残留的临时文件超过该时限即删除，防止反复失败积压占盘
        long TEMP_FILE_TTL_MILLIS = 24L * 60 * 60 * 1000;
        long expireBefore = System.currentTimeMillis() - TEMP_FILE_TTL_MILLIS;
        for (File file : files) {
            if (file.lastModified() < expireBefore) file.delete();
        }
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {
        // 上传分片以上传键作为 AAD 绑定上下文：分片无法被重放到其他上传——绑定知识属于上传方，不进入传输层
        String uploadKey = httpRequest.getParameter(Constants.AGENT_UPLOAD_FILE_KEY);
        byte[] context = uploadKey == null ? null : uploadKey.getBytes(StandardCharsets.UTF_8);
        ProcessHandler.handle(httpRequest, httpResponse, crypto, logger, context,
                requestData -> {
                    cleanExpiredUploads();
                    return processFileUpload(requestData, uploadKey);
                });
    }

    private String processFileUpload(byte[] data, String key) throws Throwable {
        String uploadId = key != null && !key.isEmpty() ? key : UUID.randomUUID().toString();
        File tempFile = resolveInUploadBase(uploadId);
        try {
            long total = tempFile.length() + (data == null ? 0 : data.length);
            long MAX_UPLOAD_BYTES = 1024L * 1024 * 1024; // 单上传总量上限：分片追加若无总量约束，认证后攻击者可持续上传写满磁盘（DoS）
            if (total > MAX_UPLOAD_BYTES) { // 超限即终止并清理，避免磁盘被写满
                tempFile.delete();
                throw new IllegalStateException("upload exceeds size limit: " + uploadId);
            }
            if (tempFile.exists()) {
                Files.write(tempFile.toPath(), data, StandardOpenOption.APPEND);
            } else {
                Files.write(tempFile.toPath(), data, StandardOpenOption.CREATE);
            }
        } catch (Throwable e) {
            tempFile.delete();
            throw e;
        }

        return uploadId;
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
