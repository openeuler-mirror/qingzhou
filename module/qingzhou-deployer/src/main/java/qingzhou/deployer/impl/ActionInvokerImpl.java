package qingzhou.deployer.impl;

import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.config.Config;
import qingzhou.config.Security;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.CryptoService;
import qingzhou.deployer.*;
import qingzhou.http.Http;
import qingzhou.http.HttpResponse;
import qingzhou.json.Json;
import qingzhou.registry.AppInfo;
import qingzhou.registry.InstanceInfo;
import qingzhou.registry.Registry;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

class ActionInvokerImpl implements ActionInvoker {
    private final Deployer deployer;
    private final Registry registry;
    private final Json json;
    private final Config config;
    private final CryptoService crypto;
    private final Http http;

    ActionInvokerImpl(Deployer deployer, Registry registry, Json json, Config config, CryptoService crypto, Http http) {
        this.deployer = deployer;
        this.registry = registry;
        this.json = json;
        this.config = config;
        this.crypto = crypto;
        this.http = http;
    }

    @Override
    public List<Response> invokeOnInstances(Request request, String... onInstances) throws Exception {
        List<Response> responseList = new ArrayList<>();
        byte[] sendContent = null;
        Cipher cipher = null;
        for (String instance : onInstances) {
            if (instance.equals(DeployerConstants.INSTANCE_LOCAL)) {
                App instanceApp = deployer.getApp(request.getApp());
                instanceApp.invoke(request);
                responseList.add(request.getResponse());
            } else {
                InstanceInfo instanceInfo = registry.getInstanceInfo(instance);
                if (sendContent == null) {
                    Security security = config.getConsole().getSecurity();
                    String remoteKey = crypto.getPairCipher(security.getPublicKey(), security.getPrivateKey())
                            .decryptWithPrivateKey(instanceInfo.getKey());
                    byte[] resultJson = json.toJson(request).getBytes(StandardCharsets.UTF_8);
                    cipher = crypto.getCipher(remoteKey);
                    sendContent = cipher.encrypt(resultJson);
                }

                String remoteUrl = "";
                try {
                    remoteUrl = String.format("http://%s:%s", instanceInfo.getHost(), instanceInfo.getPort());
                    HttpResponse response = http.buildHttpClient().send(remoteUrl, sendContent);
                    byte[] responseBody = response.getResponseBody();
                    byte[] decryptedData = cipher.decrypt(responseBody);
                    ResponseImpl result = json.fromJson(new String(decryptedData, DeployerConstants.ACTION_INVOKE_CHARSET), ResponseImpl.class);
                    responseList.add(result);
                } catch (Exception e) {
                    if (e instanceof RuntimeException || e instanceof FileNotFoundException) {
                        throw e;
                    } else {
                        throw new RuntimeException(String.format("Remote server [%s] request error: %s.",
                                remoteUrl,
                                e.getMessage()));
                    }
                }
            }
        }

        return responseList;
    }

    @Override
    public Response invokeOnce(Request request) throws Exception {
        List<String> appInstances = getAppInstances(request.getApp());
        List<Response> responseList = invokeOnInstances(request, appInstances.get(0));
        return responseList.get(0);
    }

    @Override
    public List<Response> invokeAuto(Request request) throws Exception {
        List<String> appInstances = getAppInstances(request.getApp());
        return invokeOnInstances(request, appInstances.toArray(new String[0]));
    }

    private List<String> getAppInstances(String app) {
        List<String> instances = new ArrayList<>();
        if (app == null || app.equals(DeployerConstants.APP_SYSTEM)) {
            instances.add(DeployerConstants.INSTANCE_LOCAL);
        } else {
            registry.getAllInstanceId().forEach(s -> {
                InstanceInfo instanceInfo = registry.getInstanceInfo(s);
                for (AppInfo appInfo : instanceInfo.getAppInfos()) {
                    if (appInfo.getName().equals(app)) instances.add(s);
                }
            });
        }
        return instances;
    }
}
