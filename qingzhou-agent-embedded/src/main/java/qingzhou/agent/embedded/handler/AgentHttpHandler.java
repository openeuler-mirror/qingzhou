package qingzhou.agent.embedded.handler;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.AppStubLocal;
import qingzhou.registry.Registry;

public class AgentHttpHandler implements HttpHandler {
    private final Logger logger;
    private final Json json;
    private final Registry registry;
    private final Crypto crypto;

    private volatile InstanceInfo thisInstanceInfo;

    public AgentHttpHandler(Logger logger, Json json, Registry registry, Crypto crypto) {
        this.logger = logger;
        this.json = json;
        this.registry = registry;
        this.crypto = crypto;
    }

    public void setInstanceInfo(InstanceInfo instanceInfo) {
        this.thisInstanceInfo = instanceInfo;
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {
        byte[] requestBody = httpRequest.getBody();
        if (requestBody.length == 0) return;

        InstanceInfo instanceInfo = this.thisInstanceInfo;
        if (instanceInfo == null) return;

        Cipher cipher;
        byte[] requestData;
        try {
            cipher = crypto.getCipher(instanceInfo.getKey());
            requestData = cipher.decrypt(requestBody);
        } catch (Exception e) {
            httpResponse.sendFinish("key auth error");
            return;
        }

        byte[] responseData;
        try {
            responseData = process(requestData);
        } catch (Throwable e) {
            String error = "business processing error";
            httpResponse.status500Finish(error);
            logger.error(error, e);
            return;
        }

        byte[] encrypt;
        try {
            encrypt = cipher.encrypt(responseData);
        } catch (Exception e) {
            httpResponse.status500Finish("instance Key error");
            logger.error("encryption failed: " + e.getMessage());
            return;
        }
        httpResponse.sendFinish(encrypt);
    }

    private byte[] process(byte[] data) throws Throwable {
        RequestImpl request = json.fromJson(new String(data, StandardCharsets.UTF_8), RequestImpl.class);

        AppStubLocal appStub = registry.getLocalApp(request.getApp());
        List<File> uploadDirs = uploadDirs(request, appStub);
        try {
            appStub.invokeApp(request);
        } finally {
            uploadDirs.forEach(file -> {
                try {
                    forceDelete(file);
                } catch (IOException e) {
                    logger.warn("failed to clean up the files: " + file, e);
                }
            });
        }

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
            File uploadDir = Paths.get(appStub.getAppContext().getTemp().getAbsolutePath(),
                    "UPLOAD_FILE_TEMP_SUB_DIR", uploadId).toFile();
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
                try {
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
        if (!directory.exists()) return;
        if (notSymlink(directory)) cleanDirectory(directory);
        if (!directory.delete()) {
            throw new IOException("Unable to delete directory " + directory + ".");
        }
    }

    private boolean notSymlink(File file) throws IOException {
        if (File.separatorChar == '\\') return true;
        File fileInCanonicalDir;
        if (file.getParent() == null) {
            fileInCanonicalDir = file;
        } else {
            File canonicalDir = file.getParentFile().getCanonicalFile();
            fileInCanonicalDir = new File(canonicalDir, file.getName());
        }
        return fileInCanonicalDir.getCanonicalFile().equals(fileInCanonicalDir.getAbsoluteFile());
    }

    private void cleanDirectory(File directory) throws IOException {
        if (!directory.exists()) throw new IllegalArgumentException(directory + " does not exist");
        if (!directory.isDirectory()) throw new IllegalArgumentException(directory + " is not a directory");
        File[] files = directory.listFiles();
        if (files == null) throw new IOException("failed to list contents of " + directory);
        for (File file : files) {
            forceDelete(file);
        }
    }
}