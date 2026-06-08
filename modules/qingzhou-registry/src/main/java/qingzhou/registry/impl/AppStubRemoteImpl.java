package qingzhou.registry.impl;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import qingzhou.api.Constants;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.ResponseImpl;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.Response;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.AppStubRemote;
import qingzhou.registry.service.RefreshHttpHandler;

class AppStubRemoteImpl implements AppStubRemote {
    private final InstanceInfo instanceInfo;
    private final AppMeta appMeta;
    private final Json json;
    private final HttpClient httpClient;
    private final Crypto crypto;
    private final Logger logger;

    AppStubRemoteImpl(InstanceInfo instanceInfo, AppMeta appMeta, Json json, HttpClient httpClient, Crypto crypto, Logger logger) {
        this.instanceInfo = instanceInfo;
        this.appMeta = appMeta;
        this.json = json;
        this.httpClient = httpClient;
        this.crypto = crypto;
        this.logger = logger;
    }

    @Override
    public AppMeta getAppMeta() {
        return appMeta;
    }

    @Override
    public void invokeApp(RequestImpl request) throws Throwable {
        synchronized (RefreshHttpHandler.REFRESH_KEY_LOCK) {
            invokeApp0(request);
        }
    }

    private void invokeApp0(RequestImpl request) throws Throwable {
        Response response;
        String originTargetName = request.getInstance();
        request.setInstance(Constants.LOCAL_INSTANCE_ID); // 远程到实例后，去本地实例找
        Cipher cipher = crypto.getCipher(instanceInfo.getKey());

        try {
            // 处理文件上传：将本地文件上传到远程 agent
            doFileUploads(request, cipher);

            byte[] data = json.toJson(request).getBytes(StandardCharsets.UTF_8);
            byte[] encrypted = cipher.encrypt(data);
            String agentUrl = String.format("http://%s:%s/agent/", instanceInfo.getHost(), instanceInfo.getPort());

            response = httpClient.send(httpClient.newRequest(agentUrl).body(encrypted));
            if (response.getStatus() == 200) {
                byte[] responseBody = response.getBody();
                if (responseBody != null && responseBody.length > 0) {
                    byte[] decrypted = cipher.decrypt(responseBody);
                    ResponseImpl result = json.fromJson(new String(decrypted, StandardCharsets.UTF_8), ResponseImpl.class);
                    request.setResponse(result);
                }
            } else {
                String errorMsg = "agent request failed [" + response.getStatus() + "]: " + agentUrl;
                logger.error(errorMsg);
                byte[] body = response.getBody();
                if (body != null && body.length > 0) {
                    logger.error("agent response body: " + new String(body, StandardCharsets.UTF_8));
                }
                responseError(request, errorMsg);
            }
        } catch (Exception e) {
            responseError(request, "remote processing error");
        } finally {
            request.setInstance(originTargetName);
        }
    }

    private void responseError(RequestImpl request, String error) {
        request.getResponse()
                .success(false)
                .msgLevel(qingzhou.api.Response.MsgLevel.error)
                .msg(error);
    }

    /**
     * 处理文件上传：将本地文件上传到远程 agent
     */
    private void doFileUploads(RequestImpl request, Cipher cipher) throws Exception {
        for (String field : request.getUploadFileFields()) {
            List<String> remotePaths = new ArrayList<>();
            String[] filePaths = request.getParameter(field).split(","); // 处理多文件字段（逗号分隔的路径）
            for (String path : filePaths) {
                File file = new File(path);
                String remoteFileTempKey = uploadFileToRemoteAgent(file, cipher);
                remotePaths.add(remoteFileTempKey + "=" + file.getName());
            }
            String remotePathsStr = String.join(",", remotePaths); // 将远程路径列表保存到 request
            request.getParameters().put(field, remotePathsStr); // 更新 request 中的参数为远程路径
        }
    }

    /**
     * 上传单个文件到远程 agent
     */
    private String uploadFileToRemoteAgent(File file, Cipher cipher) throws Exception {
        String fileTempKey = "";
        byte[] buffer = new byte[1024 * 8];
        try (InputStream in = Files.newInputStream(file.toPath())) {
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                byte[] encrypt = cipher.encrypt(buffer, 0, bytesRead);

                // 参考：qingzhou.agent.AgentHttpHandler.FILE_UPLOAD_URI
                String uploadUrl = String.format("http://%s:%s/agent/upload?key=" + fileTempKey, instanceInfo.getHost(), instanceInfo.getPort());

                Response uploadResult = httpClient.send(httpClient.newRequest(uploadUrl).body(encrypt));
                if (uploadResult.getStatus() != 200) {
                    String msg = "response code: " + uploadResult.getStatus() + " ";
                    byte[] body = uploadResult.getBody();
                    if (body != null) {
                        msg += new String(body, StandardCharsets.UTF_8);
                    }
                    throw new RemoteException(msg);
                } else {
                    byte[] result = cipher.decrypt(uploadResult.getBody());
                    fileTempKey = new String(result, StandardCharsets.UTF_8);
                }
            }
        }
        return fileTempKey;
    }
}
