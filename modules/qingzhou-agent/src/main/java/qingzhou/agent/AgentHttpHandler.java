package qingzhou.agent;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.http.server.HttpHandler;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.AppStubLocal;
import qingzhou.registry.Registry;

@Component(property = HttpHandler.HANDLE_PATH + "=/agent")
public class AgentHttpHandler implements HttpHandler {
    @Reference
    private Logger logger;
    @Reference
    private Json json;
    @Reference
    private Registry registry;
    @Reference
    private Crypto crypto;

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {
        byte[] requestBody = httpRequest.getBody();
        if (requestBody.length == 0) return;

        InstanceInfo thisInstanceInfo = Heartbeat.thisInstanceInfo;
        if (thisInstanceInfo == null) return; // Agent 尚未注册

        // 解密，得到请求数据
        Cipher cipher;
        byte[] requestData;
        try {
            cipher = crypto.getCipher(thisInstanceInfo.getKey());
            requestData = cipher.decrypt(requestBody);
        } catch (Exception e) {
            httpResponse.sendResponse("Key auth error !!!");
            return;
        }

        // 处理业务，得到响应数据
        byte[] responseData;
        try {
            responseData = process(requestData);
        } catch (Throwable e) {
            httpResponse.statusError()
                    .sendResponse("Business processing error !!!");
            logger.error("Business processing error !!!", e);
            return;
        }

        // 加密响应数据
        byte[] encrypt;
        try {
            encrypt = cipher.encrypt(responseData);
        } catch (Exception e) {
            httpResponse.statusError()
                    .sendResponse("Instance Key error !!!");
            logger.error("Encryption failed: " + e.getMessage());
            return;
        }
        httpResponse.sendResponse(encrypt);
    }

    private byte[] process(byte[] data) throws Throwable {
        // 3. 得到请求对象
        RequestImpl request = json.fromJson(new String(data, StandardCharsets.UTF_8), RequestImpl.class);

        // 4. 处理
        AppStubLocal appStub = registry.getLocalApp(request.getApp());
        List<File> uploadDirs = uploadDirs(request, appStub);
        try {
            appStub.invokeApp(request);
        } finally {
            uploadDirs.forEach(file -> {
                try {
                    forceDelete(file);
                } catch (IOException e) {
                    logger.warn("Failed to clean up the files: " + file, e);
                }
            });
        }

        // 响应数据
        return json.toJson(request.getResponse()).getBytes(StandardCharsets.UTF_8);
    }

    private List<File> uploadDirs(RequestImpl request, AppStubLocal appStub) {
        List<File> uploadDirs = new ArrayList<>();
        Set<String> parameterNames = request.getParameters().keySet();
        for (String uploadField : parameterNames) {
            String detectUploadFile = request.getParameter(uploadField);
            if (detectUploadFile == null ||
                    !detectUploadFile.startsWith("UPLOAD_FILE_PREFIX_FLAG")) continue;

            String uploadId = detectUploadFile.substring("UPLOAD_FILE_PREFIX_FLAG".length());
            if (uploadId.contains("..")) throw new IllegalArgumentException("Unsupported parameter: " + uploadId);
            File uploadDir = Paths.get(appStub.getAppContext().getTemp().getAbsolutePath(), "UPLOAD_FILE_TEMP_SUB_DIR", uploadId).toFile();
            uploadDirs.add(uploadDir);
            File[] listFiles = uploadDir.listFiles();
            request.getParameters().put(uploadField, Objects.requireNonNull(listFiles)[0].getAbsolutePath());
        }
        return uploadDirs;
    }

    private void forceDelete(File file) throws IOException {
        if (file.isDirectory()) {
            deleteDirectory(file);
        } else {
            if (file.exists() && !file.delete()) {
                try { // for #ITAIT-4164
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                }
                if (!file.delete()) {
                    throw new IOException("Unable to delete file: " + file);
                }
            }
        }
    }

    private void deleteDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }

        if (notSymlink(directory)) {
            cleanDirectory(directory);
        }

        if (!directory.delete()) {
            String message = "Unable to delete directory " + directory + ".";
            throw new IOException(message);
        }
    }

    private boolean notSymlink(File file) throws IOException {
        if (File.separatorChar == '\\') {
            return true;
        }
        File fileInCanonicalDir;
        if (file.getParent() == null) {
            fileInCanonicalDir = file;
        } else {
            File canonicalDir = file.getParentFile().getCanonicalFile();
            fileInCanonicalDir = new File(canonicalDir, file.getName());
        }

        return fileInCanonicalDir.getCanonicalFile().equals(fileInCanonicalDir.getAbsoluteFile());
    }

    // 将 文件夹 清空
    private void cleanDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            String message = directory + " does not exist";
            throw new IllegalArgumentException(message);
        }

        if (!directory.isDirectory()) {
            String message = directory + " is not a directory";
            throw new IllegalArgumentException(message);
        }

        File[] files = directory.listFiles();
        if (files == null) {  // null if security restricted
            throw new IOException("Failed to list contents of " + directory);
        }

        IOException exception = null;
        for (File file : files) {
            try {
                forceDelete(file);
            } catch (IOException ioe) {
                exception = ioe;
            }
        }

        if (null != exception) {
            throw exception;
        }
    }
}
