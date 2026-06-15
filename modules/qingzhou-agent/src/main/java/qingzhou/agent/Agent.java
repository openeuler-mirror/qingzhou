package qingzhou.agent;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.registry.AppStubLocal;
import qingzhou.registry.Registry;

@Component(property = HttpHandler.HANDLE_PATH + "=")
public class Agent implements HttpHandler {
    // 参考：qingzhou.registry.impl.AppStubRemoteImpl.uploadFileToRemoteAgent
    public static final String FILE_UPLOAD_URI = "/upload";
    public static final String FILE_UPLOAD_KEY = "key";
    public static final String FILE_UPLOAD_NAME_SP = "=";

    @Reference
    private Json json;
    @Reference
    private Registry registry;
    @Reference
    private Crypto crypto;

    private File uploadBase;

    @Activate
    public void init() {
        uploadBase = Paths.get(System.getProperty("qingzhou.instance"), "temp", "agent-upload").toFile();
        uploadBase.mkdirs();
    }

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
            httpResponse.status500Finish("key auth error");
            return;
        }

        // 处理业务，得到响应数据
        String responseData;
        try {
            if (httpRequest.getPath().contains(FILE_UPLOAD_URI)) {
                responseData = processFileUpload(requestData, httpRequest.getParameter(FILE_UPLOAD_KEY));
            } else {
                responseData = processRequest(requestData);
            }
        } catch (Throwable e) {
            httpResponse.status500Finish("business processing error");
            return;
        }

        // 加密响应数据
        byte[] encrypt;
        try {
            encrypt = cipher.encrypt(responseData.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            httpResponse.status500Finish("encryption failed");
            return;
        }
        httpResponse.sendFinish(encrypt);
    }

    private String processFileUpload(byte[] data, String key) throws Throwable {
        String uploadId = key != null && !key.isEmpty() ? key : UUID.randomUUID().toString();
        File tempFile = new File(uploadBase, uploadId);
        if (tempFile.exists()) {
            Files.write(tempFile.toPath(), data, StandardOpenOption.APPEND);
        } else {
            Files.write(tempFile.toPath(), data, StandardOpenOption.CREATE);
        }

        return uploadId;
    }

    private String processRequest(byte[] data) throws Throwable {
        // 3. 得到请求对象
        RequestImpl request = json.fromJson(new String(data, StandardCharsets.UTF_8), RequestImpl.class);

        // 4. 处理
        Set<String> originalFilePaths = new HashSet<>(); // 处理之前，须先保存原始文件路径，用于 finally 块中的清理，因应用可能修改此参数值
        try {
            request.getUploadFileFields().forEach(field -> {
                String newPaths = Arrays.stream(request.getParameter(field).split(",")).map(s -> {
                    String[] keyName = s.split(FILE_UPLOAD_NAME_SP);
                    File tempFile = new File(uploadBase, keyName[0]);
                    File localFile = new File(uploadBase, keyName[1]);
                    tempFile.renameTo(localFile);
                    return localFile.getAbsolutePath();
                }).collect(Collectors.joining(","));
                request.getParameters().put(field, newPaths);
                originalFilePaths.add(newPaths);
            });

            AppStubLocal appStub = registry.getLocalApp(request.getApp());
            appStub.invokeApp(request);
        } finally {
            for (String paths : originalFilePaths) {
                for (String path : paths.split(",")) {
                    File tempFile = new File(path.trim());
                    File parentDir = tempFile.getParentFile();
                    if (parentDir.equals(uploadBase)) {
                        tempFile.delete();
                    }
                }
            }
        }

        // 响应数据
        return json.toJson(request.getResponse());
    }
}
