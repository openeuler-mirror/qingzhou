package qingzhou.registry.impl;

import java.nio.charset.StandardCharsets;

import qingzhou.api.Constants;
import qingzhou.api.Response;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.ResponseImpl;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.HttpResult;
import qingzhou.json.Json;
import qingzhou.registry.AppStubRemote;

class AppStubRemoteImpl implements AppStubRemote {
    private final InstanceInfo instanceInfo;
    private final AppMeta appMeta;
    private final Json json;
    private final HttpClient httpClient;
    private final Crypto crypto;

    AppStubRemoteImpl(InstanceInfo instanceInfo, AppMeta appMeta, Json json, HttpClient httpClient, Crypto crypto) {
        this.instanceInfo = instanceInfo;
        this.appMeta = appMeta;
        this.json = json;
        this.httpClient = httpClient;
        this.crypto = crypto;
    }

    @Override
    public AppMeta getAppMeta() {
        return appMeta;
    }

    @Override
    public void invokeApp(RequestImpl request) throws Throwable {
        HttpResult response;
        String originTargetName = request.getInstance();
        request.setInstance(Constants.LOCAL_INSTANCE_ID); // 远程到实例后，去本地实例找
        Cipher cipher = crypto.getCipher(instanceInfo.getKey());
        try {
            byte[] data = json.toJson(request).getBytes(StandardCharsets.UTF_8);
            byte[] encrypted = cipher.encrypt(data);
            String agentUrl = String.format("http://%s:%s/agent", instanceInfo.getHost(), instanceInfo.getPort());
            try {
                response = httpClient.request(agentUrl, HttpMethod.POST, encrypted, null);
            } catch (Exception e) {
                ResponseImpl resp = request.getResponse();
                resp.success(false)
                        .msgLevel(Response.MsgLevel.error)
                        .msg("remote connection error");
                return;
            }
        } finally {
            request.setInstance(originTargetName);
        }
        byte[] responseBody = response.getBody();
        byte[] decrypted = cipher.decrypt(responseBody);
        ResponseImpl result = json.fromJson(new String(decrypted, StandardCharsets.UTF_8), ResponseImpl.class);
        request.setResponse(result);
    }
}
