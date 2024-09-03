package qingzhou.console;

import qingzhou.api.FieldType;
import qingzhou.api.Request;
import qingzhou.console.controller.I18n;
import qingzhou.console.controller.SystemController;
import qingzhou.console.page.PageBackendService;
import qingzhou.crypto.CryptoService;
import qingzhou.deployer.*;
import qingzhou.logger.Logger;
import qingzhou.registry.*;

import javax.naming.NameNotFoundException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.nio.file.Files;
import java.security.UnrecoverableKeyException;
import java.sql.SQLException;
import java.util.*;

public class ActionInvoker {
    static {
        I18n.addKeyI18n("batch.ops.fail", new String[]{"%s%s成功%s个，失败%s个，失败详情：", "en:%s%s success %s, failure %s, failure details:"});// 一些 filter 需要 i18n，如 LoginFreeFilter 调用了Helper.convertCommonMsg(msg)，此时 RestController 等类可能都还没有初始化（例如 Rest 直连登录），会导致 i18n 信息丢失，因此放到这里
    }

    private static final ActionInvoker instance = new ActionInvoker();

    public static ActionInvoker getInstance() {
        return instance;
    }

    private ActionInvoker() {
    }

    public void invokeAction(RequestImpl request) {
        if (isBatchAction(request)) {
            invokeBatch(request);
        } else {
            invoke(request);
        }
    }

    private boolean isBatchAction(Request request) {
        AppInfo appInfo = SystemController.getAppInfo(PageBackendService.getAppName(request));
        if (appInfo == null) return false;
        ModelInfo modelInfo = appInfo.getModelInfo(request.getModel());
        String ids = request.getParameter(modelInfo.getIdFieldName());
        if (ids == null) return false;
        String[] actionNamesSupportBatch = modelInfo.getBatchActionNames();
        for (String batch : actionNamesSupportBatch) {
            if (batch.equals(request.getAction())) return true;
        }
        return false;
    }

    private ResponseImpl invokeBatch(RequestImpl request) {
        ResponseImpl response = new ResponseImpl();
        int suc = 0;
        int fail = 0;
        StringBuilder errbuilder = new StringBuilder();
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        AppInfo appInfo = SystemController.getAppInfo(PageBackendService.getAppName(request));
        ModelInfo modelInfo = appInfo.getModelInfo(request.getModel());
        String oid = request.getParameter(modelInfo.getIdFieldName());
        for (String id : oid.split(",")) {
            if (!id.isEmpty()) {
                id = PageBackendService.decodeId(id);
                request.setId(id);
                response = invoke(request);
                if (response.isSuccess()) {
                    suc++;
                } else {
                    String actionContextMsg = response.getMsg();
                    if (result.containsKey(actionContextMsg)) {
                        errbuilder.append(result.get(actionContextMsg));
                        errbuilder.append(",");
                        errbuilder.append(id);
                        result.put(actionContextMsg, errbuilder.toString());
                        errbuilder.setLength(0);
                    } else {
                        result.put(actionContextMsg, id);
                    }
                    fail++;
                }
            }
        }
        request.setId(oid);
        String appName = PageBackendService.getAppName(request);
        String model = I18n.getModelI18n(appName, "model." + request.getModel());
        String action = I18n.getModelI18n(appName, "model.action." + request.getModel() + "." + request.getAction());
        if (result.isEmpty()) {
            String resultMsg = I18n.getKeyI18n("batch.ops.success", model, action, suc);
            response.setMsg(resultMsg);
        } else {
            response.setSuccess(suc > 0);
            errbuilder.append(I18n.getKeyI18n("batch.ops.fail", model, action, suc, fail));
            errbuilder.append("<br/>");
            for (Map.Entry<String, String> entry : result.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                errbuilder.append(value).append("= {").append(key).append("};");
                errbuilder.append("<br/>");
            }
            response.setMsg(errbuilder.toString());
        }
        return response;
    }

    private ResponseImpl invoke(RequestImpl request) {
        try {
            Map<String, ResponseImpl> responseOnNode = processRequest(request);
            for (Map.Entry<String, ResponseImpl> entry : responseOnNode.entrySet()) {
                return entry.getValue(); // TODO 多条结果如何展示？
            }

            throw new IllegalStateException("It should return at least one Response");
        } catch (Exception e) {
            ResponseImpl response = new ResponseImpl();
            response.setSuccess(false);
            Set<Throwable> set = new HashSet<>();
            retrieveException(e, set);

            // 不要将异常msg输出到页面，有xss反射型漏洞，若需要，则应该尝试完善 errorcode
            String msg = null;
            for (Throwable temp : set) {
                //这些异常一般没有xss反射风险，所以其message可作为提示信息返回到客户端
                Class<?>[] exceptions = {SQLException.class, NameNotFoundException.class, SocketException.class,
                        UnrecoverableKeyException.class};
                for (Class<?> c : exceptions) {
                    if (c.isAssignableFrom(temp.getClass())) {
                        msg = temp.getMessage();
                    }
                }

                if (msg != null) {
                    break;
                }
            }

            if (msg == null) {
                msg = "Server exception, please check log for details.";
                // 不能抛异常，否则到不了 view 处理
                SystemController.getService(Logger.class).warn(msg, e);
            }

            response.setMsg(msg);
            return response;
        }
    }

    private void retrieveException(Throwable t, Set<Throwable> set) {
        if (t == null) {
            return;
        }
        if (!set.add(t)) {
            return;
        }
        if (t.getCause() != null) {
            retrieveException(t.getCause(), set);
        }
        if (t.getSuppressed() != null) {
            for (Throwable thx : t.getSuppressed()) {
                retrieveException(thx, set);
            }
        }
    }

    private Map<String, ResponseImpl> processRequest(RequestImpl request) throws Exception {
        Map<String, ResponseImpl> resultOnNode = new HashMap<>();
        List<String> appInstances = new ArrayList<>();
        String manageType = request.getManageType();
        String appName = request.getApp();
        String uploadFileAppName = appName;
        if (DeployerConstants.MANAGE_INSTANCE.equals(manageType)) {
            appInstances.add(appName);
            uploadFileAppName = DeployerConstants.APP_INSTANCE;
        } else if (DeployerConstants.MANAGE_APP.equals(manageType)) {
            appInstances = getAppInstances(appName);
        }

        for (String instance : appInstances) {
            ResponseImpl responseOnNode = null;
            if (instance.equals("local")) {
                SystemController.getService(Deployer.class)
                        .getApp(PageBackendService.getAppName(request))
                        .invoke(request);
                responseOnNode = (ResponseImpl) request.getResponse();
            } else {
//                InstanceInfo instanceInfo = SystemController.getService(Registry.class).getInstanceInfo(instance);
//
//                String remoteUrl = String.format("http://%s:%s", instanceInfo.getHost(), instanceInfo.getPort());
//                String remoteKey = SystemController.getService(CryptoService.class).getPairCipher(SystemController.getPublicKeyString(), SystemController.getPrivateKeyString()).decryptWithPrivateKey(instanceInfo.getKey());
//
//                // 远程实例文件上传
//                uploadFile(request, uploadFileAppName, remoteUrl, remoteKey);
//
//                responseOnNode = RemoteClient.sendReq(remoteUrl, request, remoteKey);
//                // 将 response 回传的 session 参数，同步给 request
//                request.getParametersInSession().putAll(responseOnNode.getParametersInSession());
            }
            resultOnNode.put(instance, responseOnNode);
        }

        return resultOnNode;
    }

    private void uploadFile(RequestImpl request, String appName, String remoteUrl, String remoteKey) throws Exception {
        // 文件上传
        AppInfo appInfo;
        if (DeployerConstants.APP_INSTANCE.equals(appName)) {
            appInfo = SystemController.getAppInfo(appName);
        } else {
            appInfo = SystemController.getService(Registry.class).getAppInfo(appName);
        }
        if (appInfo != null) {
            ModelInfo modelInfo = appInfo.getModelInfo(request.getModel());
            if (modelInfo != null) {
                ModelFieldInfo[] modelFieldInfos = modelInfo.getModelFieldInfos();
                if (modelFieldInfos != null) {
                    for (ModelFieldInfo modelFieldInfo : modelFieldInfos) {
                        if (FieldType.file.name().equals(modelFieldInfo.getType())) {
                            String code = modelFieldInfo.getCode();
                            String fileName = request.getParameter(code);
                            if (fileName != null && !fileName.isEmpty()) {
                                String remoteFilePath = uploadTempFile(fileName, remoteUrl, remoteKey);
                                request.setParameter(code, remoteFilePath);
                            }
                        }
                    }
                }
            }
        }
    }

    private String uploadTempFile(String filePath, String remoteUrl, String remoteKey) throws Exception {
        File tempFile = new File(filePath);
        if (!tempFile.exists()
        ) {
            return null;
        }
        InputStream in = null;
        BufferedInputStream bis = null;
        try {
            String fileName = tempFile.getName();
            String timestamp = String.valueOf(System.currentTimeMillis()); // 文件标识
            long size = tempFile.length();
            int readSize = (int) size;
            int count = 1;
            int FILE_SIZE = 1024 * 1024 * 10; // 集中管控文件分割传输大小，10M
            if (size > FILE_SIZE) {
                readSize = FILE_SIZE;
                count = (int) (size / readSize);
                count = size % readSize == 0 ? count : count + 1; // 文件分片数
            }
            byte[] bytes = new byte[readSize];
            in = Files.newInputStream(tempFile.toPath());
            bis = new BufferedInputStream(in);
            int len;
//            for (int i = 0; i < count; i++) {
//                len = bis.read(bytes);
//                RequestImpl req = new RequestImpl();
//                req.setAppName(DeployerConstants.APP_INSTANCE);
//                req.setModelName("installer");
//                req.setActionName("uploadFile");
//                req.setManageType(DeployerConstants.MANAGE_APP);
//
//                Map<String, String> parameters = new HashMap<>();
//                parameters.put("fileName", fileName);
//                parameters.put("fileBytes", SystemController.getService(CryptoService.class).getBase64Coder().encode(bytes));
//                parameters.put("len", String.valueOf(len));
//                parameters.put("isStart", String.valueOf(i == 0));
//                parameters.put("isEnd", String.valueOf(i == count - 1));
//                parameters.put("timestamp", timestamp);
//                req.setParameters(parameters);
//
//                ResponseImpl response = RemoteClient.sendReq(remoteUrl, req, remoteKey);
//                if (response.isSuccess()) {
//                    List<Map<String, String>> dataList = response.getDataList();
//                    if (!dataList.isEmpty()) {
//                        return dataList.get(0).get("fileName");
//                    }
//                } else {
//                    break;
//                }
//            }
        } finally {
            try {
                if (bis != null) {
                    bis.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    private List<String> getAppInstances(String appName) {
        List<String> instances = new ArrayList<>();
        Deployer deployer = SystemController.getService(Deployer.class);
        App app = deployer.getApp(appName);
        if (app != null) {
            instances.add("local");
        }
        if (!DeployerConstants.APP_INSTANCE.equals(appName)
                && !DeployerConstants.APP_MASTER.equals(appName)) {
            try {
                Registry registry = SystemController.getService(Registry.class);
                AppInfo appInfo = registry.getAppInfo(appName);
                if (appInfo != null) {
                    for (String instanceId : registry.getAllInstanceId()) {
                        InstanceInfo instanceInfo = registry.getInstanceInfo(instanceId);
                        for (AppInfo info : instanceInfo.getAppInfos()) {
                            if (appName.equals(info.getName())) {
                                instances.add(instanceId);
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (!e.getMessage().contains("App not found:")) {
                    throw e;
                }
            }
        }

        return instances;
    }
}
