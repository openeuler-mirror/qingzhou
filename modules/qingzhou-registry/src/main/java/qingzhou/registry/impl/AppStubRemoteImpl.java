package qingzhou.registry.impl;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import qingzhou.crypto.EpochKeyRotatingCipher;
import qingzhou.dto.Constants;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.ResponseImpl;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Response;
import qingzhou.registry.AppStubRemote;

class AppStubRemoteImpl implements AppStubRemote {
    private final RegistryImpl registry;
    private final InstanceInfo instanceInfo;
    private final AppMeta appMeta;
    private final String invokeUrl;
    private final String uploadUrl;

    AppStubRemoteImpl(RegistryImpl registry, InstanceInfo instanceInfo, AppMeta appMeta) {
        this.registry = registry;
        this.instanceInfo = instanceInfo;
        this.appMeta = appMeta;

        String protocol = instanceInfo.isSslEnabled() ? "https" : "http";
        String agentBaseUrl = protocol + "://" + instanceInfo.getHost() + ":" + instanceInfo.getPort() + "/agent";
        invokeUrl = agentBaseUrl + Constants.REMOTE_INVOKE_URI;
        uploadUrl = agentBaseUrl + Constants.REMOTE_UPLOAD_URI;
    }

    @Override
    public AppMeta getAppMeta() {
        return appMeta;
    }

    @Override
    public void invokeApp(RequestImpl request) {
        byte[] agentDomainKey = registry.agentDomainKeys.getAgentDomainKey(instanceInfo.getDomain());
        EpochKeyRotatingCipher cipher = registry.crypto.getEpochKeyRotatingCipher(agentDomainKey);

        try {
            // 处理文件上传：将本地文件上传到远程 agent
            doFileUploads(request, cipher);

            byte[] data = registry.json.toJson(request).getBytes(StandardCharsets.UTF_8);
            String remoteInstanceId = request.getInstance();
            byte[] encrypted = cipher.encrypt(data, registry.agentDomainKeys.toAgentAad(remoteInstanceId, Constants.REMOTE_INVOKE_URI));
            Response response = registry.httpClient.send(registry.httpClient.buildRequest(invokeUrl)
                    .method(HttpMethod.POST)
                    .body(encrypted));
            if (response.getStatus() == 200) {
                byte[] responseBody = response.getBody();
                if (responseBody != null && responseBody.length > 0) {
                    byte[] decrypted = cipher.decrypt(responseBody, registry.agentDomainKeys.toRegistryAad(remoteInstanceId, Constants.REMOTE_INVOKE_URI));
                    ResponseImpl result = registry.json.fromJson(new String(decrypted, StandardCharsets.UTF_8), ResponseImpl.class);
                    request.setResponse(result);
                }
            } else {
                String errorMsg = "agent request failed [" + response.getStatus() + "]: " + invokeUrl;
                registry.logger.error(errorMsg);
                byte[] body = response.getBody();
                if (body != null && body.length > 0) {
                    registry.logger.error("agent response body: " + new String(body, StandardCharsets.UTF_8));
                }
                responseError(request, errorMsg);
            }
        } catch (Exception e) {
            String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            registry.logger.error("remote processing error: " + reason, e);
            responseError(request, "remote processing error: " + reason);
        }
    }

    private void responseError(RequestImpl request, String error) {
        request.getResponse()
                .success(false)
                .messageLevel(qingzhou.api.Response.MessageLevel.error)
                .message(error);
    }

    /**
     * 处理文件上传：将本地临时文件上传到远程 agent。
     * 字段值为逗号分隔列表，混合了业务已有文件名（原样传递）与本次上传的本地临时文件（上传后替换为远程键值）
     */
    private void doFileUploads(RequestImpl request, EpochKeyRotatingCipher cipher) throws Exception {
        for (String field : new ArrayList<>(request.getUploadFileFields())) {
            String value = request.getParameter(field);
            List<String> remotePaths = new ArrayList<>();
            for (String item : value.split(",")) { // TODO 应使用 ModelField 的 separator
                String path = item.trim();
                File file = toUploadTempFile(path);
                if (file == null) { // 非本次上传的本地临时文件（业务已有文件名），原样传递
                    remotePaths.add(path);
                    continue;
                }
                String remoteFileTempKey = uploadFileToRemoteAgent(file, cipher, request.getInstance());
                remotePaths.add(remoteFileTempKey + Constants.REMOTE_UPLOAD_TEMP_KEY_FILE_SP + file.getName());
            }
            String remotePathsStr = String.join(Constants.REMOTE_UPLOAD_MULTIPLE_FILE_SP, remotePaths); // 将远程路径列表保存到 request
            request.getParameters().put(field, remotePathsStr); // 更新 request 中的参数为远程路径
        }
    }

    /**
     * 仅当路径位于上传临时目录内且真实存在时才视为待上传文件：
     * 字段值可能来自 URL/JSON 等不可信来源，缺少目录约束会导致任意本地文件被读取并外传
     */
    private File toUploadTempFile(String path) {
        try {
            File file = new File(path);
            if (!file.isFile()) return null;
            String canonicalPath = file.getCanonicalPath();
            return canonicalPath.startsWith(registry.uploadBase.toFile().getCanonicalPath() + File.separator) ? file : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 上传单个文件到远程 agent
     */
    private String uploadFileToRemoteAgent(File file, EpochKeyRotatingCipher keyRotatingCipher, String remoteInstanceId) throws Exception {
        String fileTempKey = "";
        int uploadChunkBytes = 256 * 1024; // 分片大小：过小会导致大文件按 8KB 逐片独立加密 POST（请求数与开销巨大）
        byte[] buffer = new byte[uploadChunkBytes];
        try (InputStream in = Files.newInputStream(file.toPath())) {
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                byte[] chunk = Arrays.copyOf(buffer, bytesRead);
                byte[] encrypt = keyRotatingCipher.encrypt(chunk, registry.agentDomainKeys.toAgentAad(remoteInstanceId, Constants.REMOTE_UPLOAD_URI));
                Response uploadResult = registry.httpClient.send(
                        registry.httpClient.buildRequest(uploadUrl + "?" + Constants.REMOTE_PARAMETER_FILE_KEY + "=" + fileTempKey).method(HttpMethod.POST).body(encrypt));
                if (uploadResult.getStatus() != 200) {
                    String msg = "response code: " + uploadResult.getStatus() + " ";
                    byte[] body = uploadResult.getBody();
                    if (body != null) {
                        msg += new String(body, StandardCharsets.UTF_8);
                    }
                    throw new IllegalStateException(msg);
                } else {
                    byte[] result = keyRotatingCipher.decrypt(uploadResult.getBody(), registry.agentDomainKeys.toRegistryAad(remoteInstanceId, Constants.REMOTE_UPLOAD_URI));
                    fileTempKey = new String(result, StandardCharsets.UTF_8);
                }
            }
        }
        return fileTempKey;
    }
}
