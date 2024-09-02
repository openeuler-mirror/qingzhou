import qingzhou.deployer.DeployerConstants;//package qingzhou.app.master.service;
//
//import qingzhou.api.*;
//import qingzhou.api.type.Addable;
//import qingzhou.app.master.MasterApp;
//import qingzhou.config.Config;
//import qingzhou.config.Security;
//import qingzhou.crypto.CryptoService;
//import qingzhou.crypto.KeyCipher;
//import qingzhou.deployer.Deployer;
//import qingzhou.deployer.RequestImpl;
//import qingzhou.deployer.ResponseImpl;
//import qingzhou.engine.util.FileUtil;
//import qingzhou.json.Json;
//import qingzhou.registry.*;
//
//import javax.net.ssl.SSLSocketFactory;
//import java.io.*;
//import java.lang.reflect.InvocationTargetException;
//import java.net.HttpURLConnection;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.util.*;
//
//@Model(code = DeployerConstants.APP_MODEL, icon = "cube-alt",
//        menu = "Service", order = 1,
//        name = {"应用", "en:App"},
//        info = {"应用。",
//                "en:App Management."})
//public class App extends ModelBase implements Addable {
//    @ModelField(
//            required = true,
//            editable = false, createable = false,
//            list = true,
//            name = {"名称", "en:Name"},
//            info = {"应用名称。", "en:App Name"})
//    public String id;
//
//    @ModelField(
//            type = FieldType.bool,
//            name = {"使用上传", "en:Enable Upload"},
//            info = {"安装的应用可以从客户端上传，也可以从服务器端指定的位置读取。",
//                    "en:The installed app can be uploaded from the client or read from a location specified on the server side."})
//    public boolean appFrom = false;
//
//    @ModelField(
//            show = "appFrom=false",
//            required = true,
//            list = true,
//            name = {"应用位置", "en:Application File"},
//            info = {"服务器上应用程序的位置，通常是应用的程序包，注：须为 *.jar, *.zip 类型的文件或目录。",
//                    "en:The location of the application on the server, usually the app package, Note: Must be a *.jar, *.zip file or directory."})
//    public String filename;
//
//    @ModelField(
//            show = "appFrom=true",
//            type = FieldType.file,
//            required = true,
//            name = {"上传应用", "en:Upload Application"},
//            info = {"上传一个应用文件到服务器，文件须是 *.jar 或 *.zip 类型的 QingZhou 应用文件，否则可能会导致安装失败。",
//                    "en:Upload an application file to the server, the file must be a *.jar type qingzhou application file, otherwise the installation may fail."})
//    public String fromUpload;
//
//    @ModelField(
//            type = FieldType.multiselect,
//            options = {"local"},
//            list = true, //refModel = Instance.class, todo 远程获取引用model的列表
//            name = {"实例", "en:Instance"},
//            info = {"选择安装应用的实例。", "en:Select the instance where you want to install the application."})
//    public String instances = "local";
//
//    @Override
//    public void start() {
//        appContext.addI18n("app.id.not.exist", new String[]{"应用文件不存在", "en:The app file does not exist"});
//        appContext.addI18n("app.type.unknown", new String[]{"未知的应用类型", "en:Unknown app type"});
//    }
//
//    @ModelAction(
//            code =DeployerConstants.SHOW_ACTION, icon = "folder-open-alt",
//            name = {"查看", "en:Show"},
//            info = {"查看该组件的相关信息。", "en:View the information of this model."})
//    public void show(Request request) throws Exception {
//        Map<String, String> appMap = new HashMap<>();
//        String id = request.getId();
//        qingzhou.deployer.App app = MasterApp.getService(Deployer.class).getApp(id);
//        if (app != null) {
//            appMap.put("id", id);
//            appMap.put("instances", "local");
//            appMap.put("filename", ""); // ToDo
//            request.getResponse().addData(appMap);
//            return;
//        }
//
//        try {
//            Registry registry = MasterApp.getService(Registry.class);
//            Collection<String> allInstanceIds = registry.getAllInstanceId();
//            // 处理远程实例的应用信息
//            for (String instanceId : allInstanceIds) {
//                InstanceInfo instanceInfo = registry.getInstanceInfo(instanceId);
//                AppInfo[] appInfos = instanceInfo.getAppInfos();
//                for (AppInfo appInfo : appInfos) {
//                    if (id.equals(appInfo.getName())) {
//                        appMap.put("id", appInfo.getName());
//                        appMap.put("instances", instanceId);
//                        appMap.put("filename", ""); // ToDo
//                        response.addData(appMap);
//                        break;
//                    }
//                }
//                if (!appMap.isEmpty()) {
//                    break;
//                }
//            }
//        } catch (Exception ignored) {
//        }
//    }
//
//    @ModelAction(
//            name = {"更新", "en:Update"},
//            info = {"更新这个模块的配置信息。", "en:Update the configuration information for this module."})
//    public void update(Request request) throws Exception {
//        try {
//            delete(request);
//            add(request);
//        } finally {
//            ((RequestImpl) request).setManageType(DeployerConstants.APP_MANAGE);
//            ((RequestImpl) request).setAppName("master");
//            ((RequestImpl) request).setModelName(DeployerConstants.APP_MODEL);
//            ((RequestImpl) request).setActionName("update");
//        }
//    }
//
//    @ModelAction(
//            name = {"安装", "en:Install"},
//            info = {"按配置要求安装应用到指定的实例。", "en:Install the app to the specified instance as required."})
//    public void add(Request request) throws Exception {
//        String[] instances = request.getParameter("instances") != null
//                ? request.getParameter("instances").split(",")
//                : new String[0];
//        ((RequestImpl) request).setAppName(DeployerConstants.INSTANCE_APP);
//        ((RequestImpl) request).setModelName("installer");
//        ((RequestImpl) request).setActionName("installApp");
//        try {
//            Registry registry = MasterApp.getService(Registry.class);
//            Security security = MasterApp.getService(Config.class).getConsole().getSecurity();
//            for (String instance : instances) {
//                try {
//                    if ("local".equals(instance)) { // 安装到本地节点
//                        MasterApp.getService(Deployer.class).getApp(DeployerConstants.INSTANCE_APP).invokeDirectly(request, response);
//                    } else {
//                        InstanceInfo instanceInfo = registry.getInstanceInfo(instance);
//                        String remoteUrl = String.format("http://%s:%s", instanceInfo.getHost(), instanceInfo.getPort());
//                        String remoteKey = appContext.getService(CryptoService.class).getKeyPairCipher(security.getPublicKey(), security.getPrivateKey()).decryptWithPrivateKey(instanceInfo.getKey());
//
//                        uploadFile((RequestImpl) request, remoteUrl, remoteKey);
//
//                        ResponseImpl responseImpl = sendReq(remoteUrl, request, remoteKey);
//                        if (!responseImpl.isSuccess()) {
//                            System.out.println(responseImpl.getMsg());
//                        }
//                    }
//                } catch (Exception e) { // todo 部分失败，如何显示到页面？
//                    request.getResponse().setSuccess(false);
//                    if (e instanceof InvocationTargetException) {
//                        request.getResponse().setMsg(((InvocationTargetException) e).getTargetException().getMessage());
//                    } else {
//                        request.getResponse().setMsg(e.getMessage());
//                    }
//                }
//            }
//        } finally {
//            ((RequestImpl) request).setAppName("master");
//            ((RequestImpl) request).setModelName(DeployerConstants.APP_MODEL);
//            ((RequestImpl) request).setActionName(DeployerConstants.ADD_ACTION);
//        }
//    }
//
//    private static final int FILE_SIZE = 1024 * 1024 * 10; // 集中管控文件分割传输大小，10M
//
//    private void uploadFile(RequestImpl request, String remoteUrl, String remoteKey) throws Exception {
//        // 文件上传
//        qingzhou.deployer.App appInfo = MasterApp.getService(Deployer.class).getApp("master");
//        if (appInfo != null) {
//            ModelInfo modelInfo = appInfo.getAppInfo().getModelInfo(DeployerConstants.APP_MODEL);
//            if (modelInfo != null) {
//                ModelFieldInfo[] modelFieldInfos = modelInfo.getModelFieldInfos();
//                if (modelFieldInfos != null) {
//                    for (ModelFieldInfo modelFieldInfo : modelFieldInfos) {
//                        if (FieldType.file.name().equals(modelFieldInfo.getType())) {
//                            String code = modelFieldInfo.getCode();
//                            String fileName = request.getParameter(code);
//                            if (fileName != null && !fileName.isEmpty()) {
//                                String remoteFilePath = uploadTempFile(fileName, remoteUrl, remoteKey);
//                                request.setParameter(code, remoteFilePath);
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    private String uploadTempFile(String filePath, String remoteUrl, String remoteKey) throws Exception {
//        File tempFile = new File(filePath);
//        if (!tempFile.exists()
//        ) {
//            return null;
//        }
//        InputStream in = null;
//        BufferedInputStream bis = null;
//        try {
//            String fileName = tempFile.getName();
//            String timestamp = String.valueOf(System.currentTimeMillis()); // 文件标识
//            long size = tempFile.length();
//            int readSize = (int) size;
//            int count = 1;
//            if (size > FILE_SIZE) {
//                readSize = FILE_SIZE;
//                count = (int) (size / readSize);
//                count = size % readSize == 0 ? count : count + 1; // 文件分片数
//            }
//            byte[] bytes = new byte[readSize];
//            in = Files.newInputStream(tempFile.toPath());
//            bis = new BufferedInputStream(in);
//            int len;
//            for (int i = 0; i < count; i++) {
//                len = bis.read(bytes);
//                RequestImpl req = new RequestImpl();
//                req.setAppName(DeployerConstants.INSTANCE_APP);
//                req.setModelName("installer");
//                req.setActionName("uploadFile");
//                req.setManageType(DeployerConstants.APP_MANAGE);
//
//                Map<String, String> parameters = new HashMap<>();
//                parameters.put("fileName", fileName);
//                parameters.put("fileBytes", appContext.getService(CryptoService.class).getHexCoder().bytesToHex(bytes));
//                parameters.put("len", String.valueOf(len));
//                parameters.put("isStart", String.valueOf(i == 0));
//                parameters.put("isEnd", String.valueOf(i == count - 1));
//                parameters.put("timestamp", timestamp);
//                req.setParameters(parameters);
//
//                ResponseImpl response = sendReq(remoteUrl, req, remoteKey);
//                if (response.isSuccess()) {
//                    List<Map<String, String>> dataList = response.getDataList();
//                    if (!dataList.isEmpty()) {
//                        return dataList.get(0).get("fileName");
//                    }
//                } else {
//                    break;
//                }
//            }
//        } finally {
//            try {
//                if (bis != null) {
//                    bis.close();
//                }
//                if (in != null) {
//                    in.close();
//                }
//            } catch (IOException ignored) {
//            }
//        }
//        return null;
//    }
//
//    @ModelAction(
//            code  = DeployerConstants.MANAGE_ACTION,
//            show = "id!=master&id!=instance",
//            name = {"管理", "en:Manage"}, order = 1,
//            info = {"转到此应用的管理页面。", "en:Go to the administration page for this app."})
//    public void manage(Request request) throws Exception {
//    }
//
//    @ModelAction(
//            ajax = true,
//            batch = true, order = 2, show = "id!=master&id!=instance",
//            name = {"卸载", "en:Uninstall"},
//            info = {"卸载应用，只能卸载本地实例部署的应用。注：请谨慎操作，删除后不可恢复。",
//                    "en:If you uninstall an application, you can only uninstall an application deployed on a local instance. Note: Please operate with caution, it cannot be recovered after deletion."})
//    public void delete(Request request) throws Exception {
//        String appName = request.getId();
//        Deployer deployer = MasterApp.getService(Deployer.class);
//        qingzhou.deployer.App app = deployer.getApp(appName);
//
//        ((RequestImpl) request).setManageType(DeployerConstants.INSTANCE_MANAGE);
//        ((RequestImpl) request).setAppName(DeployerConstants.INSTANCE_APP);
//        ((RequestImpl) request).setModelName("installer");
//        ((RequestImpl) request).setActionName("unInstallApp");
//        try {
//            if (app != null) {
//                deployer.getApp(DeployerConstants.INSTANCE_APP).invokeDirectly(request);
//            }
//
//            // 卸载远程实例
//            Registry registry = MasterApp.getService(Registry.class);
//            AppInfo appInfo = registry.getAppInfo(appName);
//            if (appInfo != null) {
//                Security security = MasterApp.getService(Config.class).getConsole().getSecurity();
//                for (String instanceId : registry.getAllInstanceId()) {
//                    InstanceInfo instanceInfo = registry.getInstanceInfo(instanceId);
//                    for (AppInfo info : instanceInfo.getAppInfos()) {
//                        if (appName.equals(info.getName())) {
//                            ((RequestImpl) request).setAppName(instanceId);
//                            String remoteUrl = String.format("http://%s:%s", instanceInfo.getHost(), instanceInfo.getPort());
//                            String remoteKey = appContext.getService(CryptoService.class).getKeyPairCipher(security.getPublicKey(), security.getPrivateKey()).decryptWithPrivateKey(instanceInfo.getKey());
//                            ResponseImpl responseImpl = sendReq(remoteUrl, request, remoteKey);
//                            if (!responseImpl.isSuccess()) {
//                                System.out.println(responseImpl.getMsg());
//                            }
//                            break;
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            if (e.getMessage().contains("App not found:")) { // todo 部分失败，如何显示到页面？
//                return;
//            }
//            response.setSuccess(false);
//            response.setMsg(e.getMessage());
//        } finally {
//            ((RequestImpl) request).setManageType(DeployerConstants.APP_MANAGE);
//            ((RequestImpl) request).setAppName("master");
//            ((RequestImpl) request).setModelName(DeployerConstants.APP_MODEL);
//            ((RequestImpl) request).setActionName(DeployerConstants.DELETE_ACTION);
//        }
//    }
//
//    private ResponseImpl sendReq(String url, Request request, String remoteKey) throws Exception {
//        HttpURLConnection connection = null;
//        try {
//            Json jsonService = appContext.getService(Json.class);
//            String json = jsonService.toJson(request);
//
//            KeyCipher cipher;
//            try {
//                cipher = appContext.getService(CryptoService.class).getKeyCipher(remoteKey);
//            } catch (Exception ignored) {
//                throw new RuntimeException("remoteKey error");
//            }
//            byte[] encrypt = cipher.encrypt(json.getBytes(StandardCharsets.UTF_8));
//
//            connection = buildConnection(url);
//            try (OutputStream outStream = connection.getOutputStream()) {
//                outStream.write(encrypt);
//                outStream.flush();
//            }
//
//            try (InputStream inputStream = connection.getInputStream();
//                 ByteArrayOutputStream bos = new ByteArrayOutputStream(inputStream.available())) {
//                FileUtil.copyStream(inputStream, bos);
//                byte[] decryptedData = cipher.decrypt(bos.toByteArray());
//                return jsonService.fromJson(new String(decryptedData, StandardCharsets.UTF_8), ResponseImpl.class);
//            }
//        } catch (Exception e) {
//            if (e instanceof RuntimeException || e instanceof FileNotFoundException) {
//                throw e;
//            } else {
//                throw new RuntimeException(String.format("Remote server [%s] request error: %s.",
//                        url,
//                        e.getMessage()));
//            }
//        } finally {
//            if (connection != null) {
//                connection.disconnect();
//            }
//        }
//    }
//
//    private static SSLSocketFactory ssf;
//
//    @Override
//    public List<Map<String, String>> listData(int pageNum, int pageSize, String[] fieldNames) throws Exception {
//        Deployer deployer = MasterApp.getService(Deployer.class);
//        Collection<String> localAppNames = deployer.getAllApp();
//        Map<String, Set<String>> uniqueApps = new HashMap<>();
//
//        // 处理本地应用名称
//        for (String appName : localAppNames) {
//            if (qingzhou.deployer.DeployerConstants.MASTER_APP.equals(appName) ||qingzhou.deployer.DeployerConstants.INSTANCE_APP.equals(appName)) {
//                continue;
//            }
//            uniqueApps.computeIfAbsent(appName, k -> new HashSet<>()).add("local");
//        }
//
//        try {
//            // 处理远程实例的应用信息
//            Registry registry = MasterApp.getService(Registry.class);
//            for (String instanceId : registry.getAllInstanceId()) {
//                InstanceInfo instanceInfo = registry.getInstanceInfo(instanceId);
//                for (AppInfo appInfo : instanceInfo.getAppInfos()) {
//                    String appName = appInfo.getName();
//                    uniqueApps.computeIfAbsent(appName, k -> new HashSet<>()).add(instanceId);
//                }
//            }
//        } catch (Exception ignored) {
//        }
//
//        List<Map<String, String>> finalAppList = new ArrayList<>();
//        for (Map.Entry<String, Set<String>> entry : uniqueApps.entrySet()) {
//            String appName = entry.getKey();
//            Set<String> instances = entry.getValue();
//            Map<String, String> appMap = new HashMap<>();
//            appMap.put("id", appName);
//            appMap.put("instances", String.join(",", instances));
//            appMap.put("filename", !(DeployerConstants.INSTANCE_APP.equals(appName) || DeployerConstants.MASTER_APP.equals(appName)) ? "apps/" + appName : "");
//            finalAppList.add(appMap);
//        }
//
//        int totalSize = finalAppList.size();
//        int startIndex = (pageNum - 1) * pageSize;
//        int endIndex = Math.min(startIndex + pageSize, totalSize);
//
//        return finalAppList.subList(startIndex, endIndex);
//    }
//}
