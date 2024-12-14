package qingzhou.core.deployer.impl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import qingzhou.api.ActionType;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.config.console.Console;
import qingzhou.core.DeployerConstants;
import qingzhou.core.deployer.ActionInvoker;
import qingzhou.core.deployer.App;
import qingzhou.core.deployer.Deployer;
import qingzhou.core.deployer.RequestImpl;
import qingzhou.core.deployer.ResponseImpl;
import qingzhou.core.registry.AppInfo;
import qingzhou.core.registry.InstanceInfo;
import qingzhou.core.registry.ModelActionInfo;
import qingzhou.core.registry.ModelInfo;
import qingzhou.core.registry.Registry;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.CryptoService;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.Utils;
import qingzhou.http.Http;
import qingzhou.http.HttpMethod;
import qingzhou.http.HttpResponse;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.serializer.Serializer;

class ActionInvokerImpl implements ActionInvoker {
    private final ModuleContext moduleContext;
    private final Json json;
    private String privateKey;

    ActionInvokerImpl(ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
        this.json = moduleContext.getService(Json.class);
    }

    @Override
    public Response invokeOnce(Request request) {
        return invokeOnOneInstances(request).values().iterator().next();
    }

    @Override
    public Map<String, Response> invokeMultiple(Request qzRequest, String... onInstances) {
        return invokeOnInstances(qzRequest, onInstances);
    }

    @Override
    public Map<String, Response> invokeIfDistribute(Request request) {
        String appName = request.getApp();
        AppInfo appInfo = moduleContext.getService(Deployer.class).getAppInfo(appName);
        ModelInfo modelInfo = appInfo.getModelInfo(request.getModel());
        ModelActionInfo actionInfo = modelInfo.getModelActionInfo(request.getAction());
        if (actionInfo.isDistribute()) {
            // 系统应用是非注册应用，这里永远只是一个 DeployerConstants.APP_SYSTEM，所以不必设置 distribute = true
            List<String> appInstances = getAppInstances(appName);
            return invokeMultiple(request, appInstances.toArray(new String[0]));
        } else {
            return invokeOnOneInstances(request);
        }
    }

    private Map<String, Response> invokeOnOneInstances(Request request) {
        String selectInstance = selectInstance(request.getApp());
        return invokeOnInstances(request, selectInstance);
    }

    private Map<String, Response> invokeOnInstances(Request qzRequest, String... onInstances) {
        RequestImpl request = (RequestImpl) qzRequest;
        Map<String, Response> responseList = new LinkedHashMap<>();

        Map<String, File> fieldUploadFile = null;
        for (String instance : onInstances) {
            if (Utils.isBlank(instance)) continue;
            try {
                if (instance.equals(DeployerConstants.INSTANCE_LOCAL)) {
                    App instanceApp = moduleContext.getService(Deployer.class).getApp(request.getApp());
                    instanceApp.invoke(request);
                    responseList.put(instance, request.getResponse());
                } else {
                    if (privateKey == null) {
                        String consoleJson = json.toJson(((Map<String, Object>) moduleContext.getConfig()).get("console"));
                        Console console = json.fromJson(consoleJson, Console.class);
                        privateKey = console.getSecurity().getPrivateKey();
                    }
                    if (fieldUploadFile == null) {
                        fieldUploadFile = new HashMap<>();
                        AppInfo appInfo = moduleContext.getService(Deployer.class).getAppInfo(request.getApp());
                        ModelInfo modelInfo = appInfo.getModelInfo(request.getModel());
                        List<String> uploadFieldNames = getUploadFieldNames(modelInfo);
                        for (String uploadField : uploadFieldNames) {
                            String uploadFile = request.getParameter(uploadField);
                            if (Utils.notBlank(uploadFile)) {
                                fieldUploadFile.put(uploadField, new File(uploadFile));
                            }
                        }
                    }

                    InstanceInfo instanceInfo = moduleContext.getService(Registry.class).getInstanceInfo(instance);
                    String agentKey = moduleContext.getService(CryptoService.class).getPairCipher(null, privateKey).decryptWithPrivateKey(instanceInfo.getKey());
                    Cipher cipher = moduleContext.getService(CryptoService.class).getCipher(agentKey);

                    Response response;
                    String host = instanceInfo.getHost();
                    int port = instanceInfo.getPort();
                    try {
                        response = callRemoteInstance(
                                request, fieldUploadFile,
                                host, port,
                                cipher);
                    } catch (Exception e) {
                        throw new IOException(String.format("Remote Request Failed, host: %s,port: %s", host, port), e);
                    }
                    responseList.put(instance, response);
                }
            } catch (Throwable e) {
                responseList.put(instance, buildErrorResponse(instance, e));
            }
        }

        return responseList;
    }

    private static List<String> getUploadFieldNames(ModelInfo modelInfo) {
        List<String> uploadFieldNames = new LinkedList<>(Arrays.asList(modelInfo.getFileUploadFieldNames()));
        for (ModelActionInfo modelActionInfo : modelInfo.getModelActionInfos()) {
            if (modelActionInfo.getActionType() == ActionType.upload) {
                uploadFieldNames.add(modelActionInfo.getCode());
            }
        }
        return uploadFieldNames;
    }

    private Response callRemoteInstance(RequestImpl request, Map<String, File> fieldUploadFile,
                                        String host, int port, Cipher cipher) throws Exception {
        String remoteUrl = String.format("http://%s:%s", host, port);

        for (Map.Entry<String, File> e : fieldUploadFile.entrySet()) {
            String field = e.getKey();
            File file = e.getValue();
            String uploadId = UUID.randomUUID().toString().replace("-", "");

            RequestImpl tmp = new RequestImpl(request);
            tmp.setAppName(DeployerConstants.APP_SYSTEM);
            tmp.setModelName(DeployerConstants.MODEL_AGENT);
            tmp.setActionName(DeployerConstants.ACTION_UPLOAD);
            tmp.getParameters().put(DeployerConstants.UPLOAD_FILE_ID, uploadId);
            tmp.getParameters().put(DeployerConstants.UPLOAD_FILE_NAME, file.getName());
            tmp.getParameters().put(DeployerConstants.UPLOAD_APP_NAME, request.getApp());
            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                byte[] block = new byte[1024 * 1024 * 2];
                long offset = 0;
                while (true) {
                    raf.seek(offset);
                    int read = raf.read(block);
                    if (read > 0) { // ==0 表示上次正好读取到结尾
                        byte[] sendBlock =
                                read == block.length
                                        ? block
                                        : Arrays.copyOfRange(block, 0, read);
                        tmp.setByteParameter(sendBlock);
                        sendRemote(tmp, remoteUrl, cipher); // 发送附件
                        offset = raf.getFilePointer();
                    } else {
                        break;
                    }
                }
            }
            // 上传成功后，更新为远程实例上的地址
            request.getParameters().put(field, DeployerConstants.UPLOAD_FILE_PREFIX_FLAG + uploadId);
        }

        return sendRemote(request, remoteUrl, cipher); // 发送请求
    }

    private Response sendRemote(Request request, String remoteUrl, Cipher cipher) throws Exception {
        byte[] resultJson = json.toJson(request).getBytes(StandardCharsets.UTF_8);
        byte[] sendContent = cipher.encrypt(resultJson);
        HttpResponse response = moduleContext.getService(Http.class).buildHttpClient().request(remoteUrl, HttpMethod.POST, null, sendContent);
        byte[] responseBody = response.getResponseBody();
        byte[] decryptedData;
        try {
            decryptedData = cipher.decrypt(responseBody);
        } catch (Exception e) {
            decryptedData = responseBody;
        }

        return moduleContext.getService(Serializer.class).deserialize(decryptedData, ResponseImpl.class);
    }

    private Response buildErrorResponse(String instance, Throwable e) {
        ResponseImpl response = new ResponseImpl();
        response.setSuccess(false);

        Set<Throwable> test = new HashSet<>();
        Throwable tmp = e;
        while (tmp.getCause() != null && test.add(tmp)) {
            tmp = tmp.getCause();
        }
        String error = instance + ": " + (tmp.getMessage() != null ? tmp.getMessage() : "internal errors");
        response.setMsg(error);

        moduleContext.getService(Logger.class).error(error, e);
        return response;
    }

    private String selectInstance(String app) {
        List<String> appInstances = getAppInstances(app);
        if (appInstances.contains(DeployerConstants.INSTANCE_LOCAL)) {
            // 优先考虑在本地实例上执行，性能最好
            return DeployerConstants.INSTANCE_LOCAL;
        } else {
            return appInstances.get(0);
        }
    }

    private List<String> getAppInstances(String app) {
        List<String> instances = new ArrayList<>();

        App deployerApp = moduleContext.getService(Deployer.class).getApp(app);
        if (deployerApp != null) {
            instances.add(DeployerConstants.INSTANCE_LOCAL);
        }

        moduleContext.getService(Registry.class).getAllInstanceNames().forEach(s -> {
            InstanceInfo instanceInfo = moduleContext.getService(Registry.class).getInstanceInfo(s);
            for (AppInfo appInfo : instanceInfo.getAppInfos()) {
                if (appInfo.getName().equals(app)) instances.add(s);
            }
        });

        return instances;
    }
}
