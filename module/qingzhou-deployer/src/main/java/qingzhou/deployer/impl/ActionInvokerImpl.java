package qingzhou.deployer.impl;

import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.CryptoService;
import qingzhou.deployer.*;
import qingzhou.engine.util.Utils;
import qingzhou.http.Http;
import qingzhou.http.HttpResponse;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.AppInfo;
import qingzhou.registry.InstanceInfo;
import qingzhou.registry.ModelInfo;
import qingzhou.registry.Registry;

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

    ActionInvokerImpl(Deployer deployer, Registry registry, Json json, CryptoService crypto, Http http, Logger logger) {
        this.deployer = deployer;
        this.registry = registry;
        this.json = json;
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
            if (Utils.isBlank(instance)) continue;
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
                        AppInfo appInfo = registry.getAppInfo(request.getApp());
                        if (appInfo == null && request.getApp().equals(DeployerConstants.APP_SYSTEM)) {
                            // 调用远程实例上的 system app，这个不是注册来的，故从本地获取其元数据
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

                        cipher = crypto.getCipher(instanceInfo.getKey());
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
                        tmp.setNonModelParameter(DeployerConstants.UPLOAD_FILE_BYTES,
                                crypto.getBase64Coder().encode(sendBlock));
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
        if (response.getResponseCode() == 200) {
            return json.fromJson(result, ResponseImpl.class);
        } else { // 远端出现异常，不会有 json
            throw new IllegalStateException(result);
        }
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
