package qingzhou.agent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.crypto.Crypto;
import qingzhou.dto.Constants;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;

@Component(property = {HttpHandler.HANDLE_PATH + "=" + Constants.AGENT_UPLOAD_URI, HttpHandler.HANDLE_NO_AUTH + "=true"},
        service = {FileUpload.class, HttpHandler.class})
public class FileUpload implements HttpHandler {
    @Reference
    private Crypto crypto;

    private File uploadBase;

    @Activate
    public void init() {
        uploadBase = Paths.get(System.getProperty("qingzhou.instance"), "temp", "agent-upload").toFile();
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

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {
        ProcessHandler.handle(httpRequest, httpResponse, crypto, requestData -> processFileUpload(requestData, httpRequest.getParameter(Constants.AGENT_UPLOAD_KEY)));
    }

    private String processFileUpload(byte[] data, String key) throws Throwable {
        String uploadId = key != null && !key.isEmpty() ? key : UUID.randomUUID().toString();
        File tempFile = resolveInUploadBase(uploadId);
        try {
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
