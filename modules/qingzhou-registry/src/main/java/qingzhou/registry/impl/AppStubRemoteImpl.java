package qingzhou.registry.impl;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import qingzhou.crypto.EpochPacketCipher;
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
    private final String agentBaseUrl;

    AppStubRemoteImpl(RegistryImpl registry, InstanceInfo instanceInfo, AppMeta appMeta) {
        this.registry = registry;
        this.instanceInfo = instanceInfo;
        this.appMeta = appMeta;

        String protocol = instanceInfo.isSslEnabled() ? "https" : "http";
        agentBaseUrl = protocol + "://" + instanceInfo.getHost() + ":" + instanceInfo.getPort() + "/agent";
    }

    @Override
    public AppMeta getAppMeta() {
        return appMeta;
    }

    @Override
    public void invokeApp(RequestImpl request) {
        String originTargetName = request.getInstance();
        request.setInstance(Constants.LOCAL_INSTANCE_ID); // 远程到实例后，去本地实例找
        String domain = instanceInfo.getDomain();
        byte[] master = registry.agentDomainService.master(domain);
        if (master == null) {
            throw new IllegalStateException("root key not found for agent-domain: " + domain);
        }
        EpochPacketCipher packetCipher = registry.crypto.getEpochPacketCipher(master, domain);
        // AAD 绑定 域名 + 目标实例 ID：同域其它 agent 无法把本请求挪用到自己身上
        byte[] context = registry.crypto.generateCipherAad(domain, instanceInfo.getId());

        Response response = null;
        try {
            // 处理文件上传：将本地文件上传到远程 agent
            doFileUploads(request, master);

            byte[] data = registry.json.toJson(request).getBytes(StandardCharsets.UTF_8);
            byte[] encrypted = packetCipher.encrypt(data, context);

            String invokeUrl = agentBaseUrl + Constants.AGENT_INVOKE_URI;

            response = registry.httpClient.send(registry.httpClient.buildRequest(invokeUrl)
                    .method(HttpMethod.POST)
                    .body(encrypted));
            if (response.getStatus() == 200) {
                byte[] responseBody = response.getBody();
                if (responseBody != null && responseBody.length > 0) {
                    // 与 agent 侧 ProcessHandler 的响应绑定对应：以请求明文的 SHA-256 作 AAD，将响应绑定到本次请求，防止响应被替换到其他请求
                    byte[] responseAad = registry.crypto.getMessageDigest().sha256(data);
                    byte[] decrypted = packetCipher.decrypt(responseBody, responseAad);
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
        } finally {
            request.setInstance(originTargetName);
            if (response != null) {
                response.cancel();
            }
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
    private void doFileUploads(RequestImpl request, byte[] master) throws Exception {
        for (String field : new ArrayList<>(request.getUploadFileFields())) {
            String value = request.getParameter(field);
            List<String> remotePaths = new ArrayList<>();
            // 项分隔符须与 agent 侧 AgentInvoker.processRequest 的拼接保持一致
            for (String item : value.split(",")) {
                String path = item.trim();
                File file = toUploadTempFile(path);
                if (file == null) { // 非本次上传的本地临时文件（业务已有文件名），原样传递
                    remotePaths.add(path);
                    continue;
                }
                String remoteFileTempKey = uploadFileToRemoteAgent(file, master);
                remotePaths.add(remoteFileTempKey + Constants.AGENT_UPLOAD_MULTIPLE_FILE_NAME_SP + file.getName());
            }
            String remotePathsStr = String.join(Constants.AGENT_UPLOAD_MULTIPLE_FILE_FIELD_SP, remotePaths); // 将远程路径列表保存到 request
            request.getParameters().put(field, remotePathsStr); // 更新 request 中的参数为远程路径
        }
    }

    /**
     * 仅当路径位于上传临时目录内且真实存在时才视为待上传文件：
     * 字段值可能来自 URL/JSON 等不可信来源，缺少目录约束会导致任意本地文件被读取并外传
     */
    private File toUploadTempFile(String path) {
        File file = new File(path);
        if (!file.isFile()) return null;
        try {
            String canonicalPath = file.getCanonicalPath();
            return canonicalPath.startsWith(registry.uploadBase.toFile().getCanonicalPath() + File.separator) ? file : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 上传单个文件到远程 agent
     */
    private String uploadFileToRemoteAgent(File file, byte[] master) throws Exception {
        String fileTempKey = "";
        int uploadChunkBytes = 256 * 1024; // 分片大小：过小会导致大文件按 8KB 逐片独立加密 POST（请求数与开销巨大）
        byte[] buffer = new byte[uploadChunkBytes];
        try (InputStream in = Files.newInputStream(file.toPath())) {
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                // 只加密本次实际读到的字节：直接传 buffer 会把上一片的残留字节一起发送，导致远端文件损坏。
                // 分片以上传键作为绑定上下文：分片无法被重放到其他上传或拼接
                byte[] chunk = Arrays.copyOf(buffer, bytesRead);
                // 每片独立取时并派生密钥：分片可跨时段，长上传不会因密钥过期而失败
                EpochPacketCipher packetCipher = registry.crypto.getEpochPacketCipher(master, instanceInfo.getDomain());
                byte[] chunkContext = registry.crypto.generateCipherAad(instanceInfo.getDomain(), instanceInfo.getId(), fileTempKey);
                byte[] encrypt = packetCipher.encrypt(chunk, chunkContext);

                // 参考：qingzhou.agent.FileUpload 中的 Constants.AGENT_UPLOAD_URI
                String uploadUrl = agentBaseUrl + Constants.AGENT_UPLOAD_URI + "?" + Constants.AGENT_UPLOAD_FILE_KEY + "=" + fileTempKey;

                Response uploadResult = registry.httpClient.send(
                        registry.httpClient.buildRequest(uploadUrl).method(HttpMethod.POST).body(encrypt));
                try {
                    if (uploadResult.getStatus() != 200) {
                        String msg = "response code: " + uploadResult.getStatus() + " ";
                        byte[] body = uploadResult.getBody();
                        if (body != null) {
                            msg += new String(body, StandardCharsets.UTF_8);
                        }
                        throw new IllegalStateException(msg);
                    } else {
                        // 上传响应同样绑定请求分片内容（agent 侧 ProcessHandler 以请求明文 SHA-256 作 AAD）
                        byte[] responseAad = registry.crypto.getMessageDigest().sha256(chunk);
                        byte[] result = packetCipher.decrypt(uploadResult.getBody(), responseAad);
                        fileTempKey = new String(result, StandardCharsets.UTF_8);
                    }
                } finally {
                    uploadResult.cancel();
                }
            }
        }
        return fileTempKey;
    }
}
