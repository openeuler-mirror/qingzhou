package qingzhou.registry.impl;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import qingzhou.crypto.EpochKeyRotatingCipher;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.ResponseImpl;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Response;
import qingzhou.registry.AppStubRemote;
import qingzhou.registry.Constants;

class AppStubRemoteImpl implements AppStubRemote {
    private static final int UPLOAD_CHUNK_BYTES = 256 * 1024; // 分片过小会让大文件按 8KB 逐片独立加密 POST，请求数激增

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
        EpochKeyRotatingCipher cipher = registry.agentDomainService.getInstanceCipher(instanceInfo.getDomain(), instanceInfo.getInstanceSecret());
        try {
            doFileUploads(request, cipher);

            byte[] data = registry.json.toJson(request).getBytes(StandardCharsets.UTF_8);
            byte[] aad = registry.agentDomainService.aadForAgent(instanceInfo.getId(), Constants.REMOTE_INVOKE_URI);
            byte[] encrypted = cipher.encrypt(data, aad);
            Response response = registry.httpClient.send(registry.httpClient.buildRequest(invokeUrl)
                    .method(HttpMethod.POST)
                    .body(encrypted));
            if (response.getStatus() == 200) {
                byte[] responseBody = response.getBody();
                if (responseBody != null && responseBody.length > 0) {
                    byte[] decrypted = cipher.decrypt(responseBody, registry.agentDomainService.responseAad(aad));
                    request.setResponse(registry.json.fromJson(new String(decrypted, StandardCharsets.UTF_8), ResponseImpl.class));
                }
                return;
            }

            byte[] body = response.getBody();
            registry.logger.error("agent request failed [" + response.getStatus() + "]: " + invokeUrl
                    + (body == null || body.length == 0 ? "" : ", body: " + preview(body)));
            responseError(request, "remote request failed [" + response.getStatus() + "]");
        } catch (Exception e) {
            registry.logger.error("remote processing error: " + invokeUrl, e);
            responseError(request, "remote request failed");
        }
    }

    // 对外只回通用错误：内部地址、异常原文只进日志
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
            if (value == null) continue;

            List<String> remotePaths = new ArrayList<>();
            for (String item : value.split(Constants.REMOTE_UPLOAD_MULTIPLE_FILE_SP)) {
                String path = item.trim();
                File file = toUploadTempFile(path);
                if (file == null) { // 非本次上传的本地临时文件（业务已有文件名），原样传递
                    remotePaths.add(path);
                    continue;
                }
                String remoteKey = uploadFileToRemoteAgent(file, cipher);
                remotePaths.add(remoteKey + Constants.REMOTE_UPLOAD_TEMP_KEY_FILE_SP + file.getName());
            }
            request.getParameters().put(field, String.join(Constants.REMOTE_UPLOAD_MULTIPLE_FILE_SP, remotePaths));
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
     * 分片上传单个文件：偏移量随分片一起加密，Agent 侧按偏移量校验连续性，丢片或乱序都会被拒。
     */
    private String uploadFileToRemoteAgent(File file, EpochKeyRotatingCipher cipher) throws Exception {
        String key = "";
        long offset = 0;
        byte[] buffer = new byte[UPLOAD_CHUNK_BYTES];
        try (InputStream in = Files.newInputStream(file.toPath())) {
            int bytesRead = Math.max(in.read(buffer), 0); // 空文件也要发一个分片，用于建立远端临时文件
            while (true) {
                byte[] chunk = bytesRead == buffer.length ? buffer : Arrays.copyOf(buffer, bytesRead);
                key = sendChunk(key, offset, chunk, cipher);
                offset += bytesRead;
                if (bytesRead < buffer.length) break;
                bytesRead = in.read(buffer);
                if (bytesRead < 0) break;
            }
        }
        return key;
    }

    private String sendChunk(String key, long offset, byte[] chunk, EpochKeyRotatingCipher cipher) throws Exception {
        byte[] aad = registry.agentDomainService.aadForAgent(instanceInfo.getId(), Constants.REMOTE_UPLOAD_URI, key, String.valueOf(offset));
        byte[] encrypted = cipher.encrypt(chunk, aad);
        String url = uploadUrl + "?" + Constants.REMOTE_PARAMETER_FILE_KEY + "=" + encode(key)
                + "&" + Constants.REMOTE_PARAMETER_FILE_OFFSET + "=" + offset;
        Response response = registry.httpClient.send(registry.httpClient.buildRequest(url).method(HttpMethod.POST).body(encrypted));
        if (response.getStatus() != 200) {
            throw new IllegalStateException("upload failed [" + response.getStatus() + "]: " + uploadUrl);
        }
        byte[] body = response.getBody();
        if (body == null || body.length == 0) {
            throw new IllegalStateException("empty upload response: " + uploadUrl);
        }
        byte[] decrypted = cipher.decrypt(body, registry.agentDomainService.responseAad(aad));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private static String encode(String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value, "UTF-8"); // Java 8 无 Charset 重载
    }

    // 远端响应体可能很大或含控制字符：截断并清洗后再入日志
    private static String preview(byte[] body) {
        int len = Math.min(body.length, 256);
        return new String(body, 0, len, StandardCharsets.UTF_8).replaceAll("\\p{Cntrl}", "?");
    }
}
