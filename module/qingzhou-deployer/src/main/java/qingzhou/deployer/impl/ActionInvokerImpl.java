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
import qingzhou.logger.Logger;
import qingzhou.registry.AppInfo;
import qingzhou.registry.InstanceInfo;
import qingzhou.registry.ModelInfo;
import qingzhou.registry.Registry;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

class ActionInvokerImpl implements ActionInvoker {
    private final Deployer deployer;
    private final Registry registry;
    private final Json json;
    private final Config config;
    private final CryptoService crypto;
    private final Http http;
    private final Logger logger;

    ActionInvokerImpl(Deployer deployer, Registry registry, Json json, Config config, CryptoService crypto, Http http, Logger logger) {
        this.deployer = deployer;
        this.registry = registry;
        this.json = json;
        this.config = config;
        this.crypto = crypto;
        this.http = http;
        this.logger = logger;
    }

    @Override
    public List<Response> invokeOnInstances(Request request, String... onInstances) {
        List<Response> responseList = new ArrayList<>();

        Cipher cipher = null;
        Map<String, File> fieldUploadFile = new HashMap<>();
        for (String instance : onInstances) {
            try {
                if (instance.equals(DeployerConstants.INSTANCE_LOCAL)) {
                    App instanceApp = deployer.getApp(request.getApp());
                    AppContextImpl appContext = (AppContextImpl) instanceApp.getAppContext();
                    appContext.setRequestLang(request.getLang());
                    instanceApp.invoke(request);
                    responseList.add(request.getResponse());
                } else {
                    InstanceInfo instanceInfo = registry.getInstanceInfo(instance);
                    if (cipher == null) {
                        Security security = config.getConsole().getSecurity();
                        String remoteKey = crypto.getPairCipher(security.getPublicKey(), security.getPrivateKey())
                                .decryptWithPrivateKey(instanceInfo.getKey());

                        AppInfo appInfo = registry.getAppInfo(request.getApp());
                        ModelInfo modelInfo = appInfo.getModelInfo(request.getModel());
                        String[] uploadFieldNames = modelInfo.getFileUploadFieldNames();
                        if (uploadFieldNames != null) {
                            for (String uploadField : uploadFieldNames) {
                                String uploadFile = request.getParameter(uploadField);
                                fieldUploadFile.put(uploadField, new File(uploadFile));
                            }
                        }

                        cipher = crypto.getCipher(remoteKey);
                    }

                    Response response = callRemoteInstance(instanceInfo.getHost(), instanceInfo.getPort(),
                            fieldUploadFile, cipher);
                    responseList.add(response);
                }
            } catch (Exception e) {
                responseList.add(buildErrorResponse(instance, e));
            }
        }

        return responseList;
    }

    private Response callRemoteInstance(String host, int port, Map<String, File> fieldUploadFile, Cipher cipher) throws Exception {
        String remoteUrl = String.format("http://%s:%s", host, port);

//        fieldUploadFile.entrySet().forEach(new Consumer<Map.Entry<String, File>>() {
//            @Override
//            public void accept(Map.Entry<String, File> e) {
//                String field = e.getKey();
//                File uploadFile = e.getValue();
//                RequestImpl tmp = new RequestImpl();
//                tmp.setAppName(DeployerConstants.APP_SYSTEM);
//                tmp.setModelName(DeployerConstants.MODEL_AGENT);
//                tmp.setActionName(DeployerConstants.ACTION_UPLOAD);
//                tmp.setNonModelParameter(DeployerConstants.INSTALLER_PARAMETER_FILE_ID, );
//                tmp.setNonModelParameter(DeployerConstants.INSTALLER_PARAMETER_FILE_NAME, );
//                tmp.setNonModelParameter(DeployerConstants.INSTALLER_PARAMETER_FILE_BYTES, );
//                sendRemote(xx, remoteUrl, cipher); // 发送附件
//            }
//        });
//
//        return sendRemote(xx, remoteUrl, cipher); // 发送请求
        return null;
    }

    private Response sendRemote(Request request, String remoteUrl, Cipher cipher) throws Exception {
        byte[] resultJson = json.toJson(request).getBytes(StandardCharsets.UTF_8);
        byte[] sendContent = cipher.encrypt(resultJson);
        HttpResponse response = http.buildHttpClient().send(remoteUrl, sendContent);
        byte[] responseBody = response.getResponseBody();
        byte[] decryptedData = cipher.decrypt(responseBody);
        return json.fromJson(new String(decryptedData, DeployerConstants.ACTION_INVOKE_CHARSET), ResponseImpl.class);
    }

    private Response buildErrorResponse(String instance, Exception e) {
        ResponseImpl response = new ResponseImpl();
        response.setSuccess(false);

        Set<Throwable> test = new HashSet<>();
        Throwable tmp = e;
        while (tmp.getCause() != null && test.add(tmp)) {
            tmp = tmp.getCause();
        }
        String error = instance + ": " + tmp.getMessage();
        response.setMsg(error);

        logger.error(error, e);
        return response;
    }

    @Override
    public Response invokeSingle(Request request) {
        List<String> appInstances = getAppInstances(request.getApp());
        List<Response> responseList = invokeOnInstances(request, appInstances.get(0));
        return responseList.get(0);
    }

    @Override
    public List<Response> invokeAll(Request request) {
        List<String> appInstances = getAppInstances(request.getApp());
        return invokeOnInstances(request, appInstances.toArray(new String[0]));
    }

    private List<String> getAppInstances(String app) {
        List<String> instances = new ArrayList<>();

        App deployerApp = deployer.getApp(app);
        if (deployerApp != null) {
            instances.add(DeployerConstants.INSTANCE_LOCAL);
        } else {
            registry.getAllInstanceNames().forEach(s -> {
                InstanceInfo instanceInfo = registry.getInstanceInfo(s);
                for (AppInfo appInfo : instanceInfo.getAppInfos()) {
                    if (appInfo.getName().equals(app)) instances.add(s);
                }
            });
        }
        return instances;
    }
}
