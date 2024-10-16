package qingzhou.deployer.impl;

import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.config.Config;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.CryptoService;
import qingzhou.deployer.*;
import qingzhou.engine.util.Utils;
import qingzhou.http.Http;
import qingzhou.http.HttpResponse;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.*;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.*;

class ActionInvokerImpl implements ActionInvoker {
    private final Deployer deployer;
    private final Registry registry;
    private final Json json;
    private final CryptoService crypto;
    private final Http http;
    private final Logger logger;
    private final Config config;

    ActionInvokerImpl(Deployer deployer, Registry registry, Json json, CryptoService crypto, Http http, Logger logger, Config config) {
        this.deployer = deployer;
        this.registry = registry;
        this.json = json;
        this.crypto = crypto;
        this.http = http;
        this.logger = logger;
        this.config = config;
    }

    @Override
    public List<Response> invokeOnInstances(Request request, String... onInstances) {
        List<Response> responseList = new ArrayList<>();

        Cipher cipher = null;
        Map<String, File> fieldUploadFile = new HashMap<>();
        for (String instance : onInstances) {
            if (Utils.isBlank(instance)) continue;
            try {
                if (instance.equals(DeployerConstants.INSTANCE_LOCAL)) {
                    App instanceApp = deployer.getApp(request.getApp());
                    AppContextImpl appContext = (AppContextImpl) instanceApp.getAppContext();
                    appContext.setCurrentRequest(request);
                    instanceApp.invoke(request);
                    responseList.add(request.getResponse());
                } else {
                    InstanceInfo instanceInfo = registry.getInstanceInfo(instance);
                    if (cipher == null) {
                        AppInfo appInfo = registry.getAppInfo(request.getApp());
                        if (appInfo == null && request.getApp().equals(DeployerConstants.APP_SYSTEM)) {
                            // 调用远程实例上的 system app，这个不是注册来的，故需从本地获取其元数据，如远程上传文件
                            appInfo = deployer.getApp(request.getApp()).getAppInfo();
                        }
                        ModelInfo modelInfo = Objects.requireNonNull(appInfo).getModelInfo(request.getModel());
                        String[] uploadFieldNames = modelInfo.getFileUploadFieldNames();
                        if (uploadFieldNames != null) {
                            for (String uploadField : uploadFieldNames) {
                                String uploadFile = request.getParameter(uploadField);
                                if (Utils.notBlank(uploadFile)) {
                                    fieldUploadFile.put(uploadField, new File(uploadFile));
                                }
                            }
                        }

                        String privateKey = config.getConsole().getSecurity().getPrivateKey();
                        String agentKey = crypto.getPairCipher(null, privateKey).decryptWithPrivateKey(instanceInfo.getKey());
                        cipher = crypto.getCipher(agentKey);
                    }

                    Response response = callRemoteInstance(
                            request, fieldUploadFile,
                            instanceInfo.getHost(), instanceInfo.getPort(),
                            cipher);
                    responseList.add(response);
                }
            } catch (Exception e) {
                responseList.add(buildErrorResponse(instance, e));
            }
        }

        return responseList;
    }

    private Response callRemoteInstance(Request request, Map<String, File> fieldUploadFile,
                                        String host, int port, Cipher cipher) throws Exception {
        String remoteUrl = String.format("http://%s:%s", host, port);

        for (Map.Entry<String, File> e : fieldUploadFile.entrySet()) {
            String field = e.getKey();
            File file = e.getValue();
            String uploadId = UUID.randomUUID().toString().replace("-", "");

            RequestImpl tmp = new RequestImpl();
            tmp.setAppName(DeployerConstants.APP_SYSTEM);
            tmp.setModelName(DeployerConstants.MODEL_AGENT);
            tmp.setActionName(DeployerConstants.ACTION_UPLOAD);
            tmp.setNonModelParameter(DeployerConstants.UPLOAD_FILE_ID, uploadId);
            tmp.setNonModelParameter(DeployerConstants.UPLOAD_FILE_NAME, file.getName());
            tmp.setNonModelParameter(DeployerConstants.UPLOAD_APP_NAME, request.getApp());
            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                byte[] block = new byte[DeployerConstants.DOWNLOAD_BLOCK_SIZE];
                long offset = 0;
                while (true) {
                    raf.seek(offset);
                    int read = raf.read(block);
                    if (read > 0) { // ==0 表示上次正好读取到结尾
                        byte[] sendBlock =
                                read == DeployerConstants.DOWNLOAD_BLOCK_SIZE
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
            ((RequestImpl) request).setParameter(field, DeployerConstants.UPLOAD_FILE_PREFIX_FLAG + uploadId);
        }

        return sendRemote(request, remoteUrl, cipher); // 发送请求
    }

    private Response sendRemote(Request request, String remoteUrl, Cipher cipher) throws Exception {
        byte[] resultJson = json.toJson(request).getBytes(StandardCharsets.UTF_8);
        byte[] sendContent = cipher.encrypt(resultJson);
        HttpResponse response = http.buildHttpClient().send(remoteUrl, sendContent);
        byte[] responseBody = response.getResponseBody();
        byte[] decryptedData;
        try {
            decryptedData = cipher.decrypt(responseBody);
        } catch (Exception e) {
            decryptedData = responseBody;
        }
        String result = new String(decryptedData, DeployerConstants.ACTION_INVOKE_CHARSET);
        return json.fromJson(result, ResponseImpl.class);
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
        List<Response> responseList;
        List<String> appInstances = getAppInstances(request.getApp());
        if (appInstances.contains(DeployerConstants.INSTANCE_LOCAL)) {
            // 优先考虑在本地实例上执行，性能最好
            responseList = invokeOnInstances(request, DeployerConstants.INSTANCE_LOCAL);
        } else {
            responseList = invokeOnInstances(request, appInstances.get(0));
        }
        return responseList.get(0);
    }

    @Override
    public List<Response> invoke(Request request) {
        String appName = request.getApp();
        AppInfo appInfo = deployer.getAppInfo(appName);
        ModelInfo modelInfo = appInfo.getModelInfo(request.getModel());
        ModelActionInfo actionInfo = modelInfo.getModelActionInfo(request.getAction());
        if (actionInfo.isDistribute()) {
            List<String> appInstances = getAppInstances(appName);
            return invokeOnInstances(request, appInstances.toArray(new String[0]));
        } else {
            return Collections.singletonList(invokeSingle(request));
        }
    }

    private List<String> getAppInstances(String app) {
        List<String> instances = new ArrayList<>();

        App deployerApp = deployer.getApp(app);
        if (deployerApp != null) {
            instances.add(DeployerConstants.INSTANCE_LOCAL);
        }

        registry.getAllInstanceNames().forEach(s -> {
            InstanceInfo instanceInfo = registry.getInstanceInfo(s);
            for (AppInfo appInfo : instanceInfo.getAppInfos()) {
                if (appInfo.getName().equals(app)) instances.add(s);
            }
        });

        return instances;
    }
}
