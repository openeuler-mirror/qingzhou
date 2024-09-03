//package qingzhou.app.master.service;
//
//import qingzhou.api.*;
//import qingzhou.api.type.Listable;
//import qingzhou.app.master.Main;
//import qingzhou.config.Config;
//import qingzhou.config.Security;
//import qingzhou.crypto.Cipher;
//import qingzhou.crypto.CryptoService;
//import qingzhou.deployer.Deployer;
//import qingzhou.deployer.DeployerConstants;
//import qingzhou.deployer.RequestImpl;
//import qingzhou.deployer.ResponseImpl;
//import qingzhou.engine.util.FileUtil;
//import qingzhou.json.Json;
//import qingzhou.registry.*;
//
//import java.io.*;
//import java.net.HttpURLConnection;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Model(code = DeployerConstants.MODEL_APP, icon = "cube-alt",
//        menu = "Service", order = 1,
//        name = {"应用", "en:App"},
//        info = {"应用，是一种按照“轻舟应用开发规范”编写的软件包，可部署在轻舟平台上，用于管理特定的业务系统。",
//                "en:Application is a software package written in accordance with the \"Qingzhou Application Development Specification\", which can be deployed on the Qingzhou platform and used to manage specific business systems."})
//public class App extends ModelBase implements Listable {
//    private final String SP = DeployerConstants.DEFAULT_DATA_SEPARATOR;
//
//    @Override
//    public String idFieldName() {
//        return DeployerConstants.APP_KEY_ID;
//    }
//
//    @ModelField(
//            editable = false,
//            list = true,
//            name = {"名称", "en:Name"},
//            info = {"应用的名称信息，用以识别业务系统。",
//                    "en:The name of the application to identify the business system."})
//    public String id;
//
//    @ModelField(
//            type = FieldType.bool,
//            name = {"使用上传", "en:Enable Upload"},
//            info = {"安装的应用可以从客户端上传，也可以从服务器端指定的位置读取。",
//                    "en:The installed app can be uploaded from the client or read from a location specified on the server side."})
//    public boolean upload = false;
//
//    @ModelField(
//            show = "upload=false",
//            required = true,
//            filePath = true,
//            list = true,
//            name = {"应用位置", "en:Application File"},
//            info = {"服务器上应用程序的位置，通常是应用的程序包，注：须为 *.jar, *.zip 类型的文件或目录。",
//                    "en:The location of the application on the server, usually the app package, Note: Must be a *.jar, *.zip file or directory."})
//    public String path;
//
//    @ModelField(
//            show = "upload=true",
//            type = FieldType.file,
//            required = true,
//            name = {"上传应用", "en:Upload Application"},
//            info = {"上传一个应用文件到服务器，文件须是 *.jar 或 *.zip 类型的 QingZhou 应用文件，否则可能会导致安装失败。",
//                    "en:Upload an application file to the server, the file must be a *.jar type qingzhou application file, otherwise the installation may fail."})
//    public String file;
//
//    @ModelField(
//            type = FieldType.multiselect,
//            required = true,
//            options = {DeployerConstants.INSTANCE_LOCAL},
//            list = true, //refModel = Instance.class, todo 远程获取引用model的列表
//            name = {"实例", "en:Instance"},
//            info = {"选择安装应用的实例。", "en:Select the instance where you want to install the application."})
//    public String instances = DeployerConstants.INSTANCE_LOCAL;
//
//    @Override
//    public void start() {
//        appContext.addI18n("app.id.not.exist", new String[]{"应用文件不存在", "en:The app file does not exist"});
//        appContext.addI18n("app.type.unknown", new String[]{"未知的应用类型", "en:Unknown app type"});
//    }
//
//    @Override
//    public Map<String, String> showData(String id) {
//        qingzhou.deployer.App app = Main.getService(Deployer.class).getApp(id);
//        if (app != null) {
//            Map<String, String> appMap = new HashMap<>();
//            appMap.put(idFieldName(), id);
//            appMap.put("instances", DeployerConstants.INSTANCE_LOCAL);
//            appMap.put(DeployerConstants.APP_KEY_PATH, app.getAppContext().getAppDir().getAbsolutePath());
//            return appMap;
//        }
//
//        Registry registry = Main.getService(Registry.class);
//        Map<String, String> appMap = new HashMap<>();
//        for (String instance : registry.getAllInstanceId()) {
//            InstanceInfo instanceInfo = registry.getInstanceInfo(instance);
//            for (AppInfo appInfo : instanceInfo.getAppInfos()) {
//                if (appInfo.getName().equals(id)) {
//                    if (!appMap.containsKey(idFieldName())) {
//                        appMap.put(idFieldName(), id);
//                        appMap.put("instances", instanceInfo.getId());
//                        appMap.put(DeployerConstants.APP_KEY_PATH, appInfo.getFilePath());
//                    } else {
//                        appMap.put("instances", appMap.get("instances")
//                                + SP + instanceInfo.getId());
//                    }
//                }
//            }
//        }
//
//        return null;
//    }
//
//    @Override
//    public List<Map<String, String>> listData(int pageNum, int pageSize, String[] fieldNames) {
//        List<String> allAppNames = listAllAppNames();
//        int totalSize = allAppNames.size();
//        int startIndex = (pageNum - 1) * pageSize;
//        int endIndex = Math.min(startIndex + pageSize, totalSize);
//        List<String> subList = allAppNames.subList(startIndex, endIndex);
//        List<Map<String, String>> data = new ArrayList<>();
//        subList.forEach(a -> data.add(showData(a)));
//        return data;
//    }
//
//    private List<String> listAllAppNames() {
//        List<String> allAppNames = new ArrayList<>();
//
//        Main.getService(Deployer.class).getAllApp().forEach(a -> {
//            if (DeployerConstants.APP_MASTER.equals(a)
//                    || DeployerConstants.APP_INSTANCE.equals(a)) {
//                return;
//            }
//            allAppNames.add(a);
//        });
//
//        Registry registry = Main.getService(Registry.class);
//        allAppNames.addAll(registry.getAllAppNames());
//
//        return allAppNames;
//    }
//
//    @ModelAction(
//            code = DeployerConstants.ACTION_DELETE, icon = "trash", order = 9,
//            ajax = true,
//            batch = true,
//            name = {"卸载", "en:Uninstall"},
//            info = {"卸载应用，只能卸载本地实例部署的应用。注：请谨慎操作，删除后不可恢复。",
//                    "en:If you uninstall an application, you can only uninstall an application deployed on a local instance. Note: Please operate with caution, it cannot be recovered after deletion."})
//    public void delete(Request request) throws Exception {
//        String appName = request.getId();
//        Map<String, String> app = showData(appName);
//        String[] instances = app.get("instances").split(SP);
//
//        RequestImpl tmpReq = new RequestImpl();
//        tmpReq.setId(appName);
//        tmpReq.setAppName(DeployerConstants.APP_INSTANCE);
//        tmpReq.setModelName(DeployerConstants.MODEL_INSTALLER);
//        tmpReq.setActionName(DeployerConstants.ACTION_UNINSTALL);
//
//        List<Response> responseList = invokeOnInstances(tmpReq, instances);
//        request.getResponse().setSuccess(responseList.isEmpty());
//
//        if (!responseList.isEmpty()) {
//            // todo 参考 ActionInvoker 的 invokeBatch 方法，给出友好的响应信息
//        }
//    }
//
//    @ModelAction(
//            code = DeployerConstants.ACTION_UPDATE, icon = "save",
//            ajax = true,
//            name = {"更新", "en:Update"},
//            info = {"更新这个模块的配置信息。", "en:Update the configuration information for this module."})
//    public void update(Request request) throws Exception {
//        delete(request);
//        add(request);
//    }
//
//    @ModelAction(
//            code = DeployerConstants.ACTION_ADD, icon = "save",
//            ajax = true,
//            name = {"添加", "en:Add"},
//            info = {"按配置要求创建一个模块。", "en:Create a module as configured."})
//    public void add(Request request) throws Exception {
//        String[] instances = request.getParameter("instances").split(SP);
//
//        RequestImpl tmpReq = new RequestImpl();
//        tmpReq.setAppName(DeployerConstants.APP_INSTANCE);
//        tmpReq.setModelName(DeployerConstants.MODEL_INSTALLER);
//        tmpReq.setActionName(DeployerConstants.ACTION_INSTALL);
//        tmpReq.setParameters(request.getParameters());
//
//        List<Response> responseList = invokeOnInstances(tmpReq, instances);
//        request.getResponse().setSuccess(responseList.isEmpty());
//
//        if (!responseList.isEmpty()) {
//            // todo 参考 ActionInvoker 的 invokeBatch 方法，给出友好的响应信息
//        }
//    }
//
//    @ModelAction(
//            code = DeployerConstants.ACTION_MANAGE, icon = "location-arrow",
//            order = 1,
//            name = {"管理", "en:Manage"},
//            info = {"转到此应用的管理页面。", "en:Go to the administration page for this app."})
//    public void manage(Request request) {
//    }
//
//    private List<Response> invokeOnInstances(Request tmpReq, String[] instances) throws Exception {
//        List<Response> responseList = new ArrayList<>();
//
//        String remoteKey = null;
//        for (String instance : instances) {
//            if (instance.equals(DeployerConstants.INSTANCE_LOCAL)) {
//                Deployer deployer = Main.getService(Deployer.class);
//                qingzhou.deployer.App instanceApp = deployer.getApp(tmpReq.getApp());
//                instanceApp.invokeDirectly(tmpReq);
//                responseList.add(tmpReq.getResponse());
//            } else {
//                Registry registry = Main.getService(Registry.class);
//                InstanceInfo instanceInfo = registry.getInstanceInfo(instance);
//                String remoteUrl = String.format("http://%s:%s", instanceInfo.getHost(), instanceInfo.getPort());
//
//                if (remoteKey == null) {
//                    Config config = Main.getService(Config.class);
//                    Security security = config.getConsole().getSecurity();
//                    remoteKey = Main.getService(CryptoService.class)
//                            .getPairCipher(security.getPublicKey(), security.getPrivateKey())
//                            .decryptWithPrivateKey(instanceInfo.getKey());
//                }
//
//                ResponseImpl responseImpl = sendReq(remoteUrl, tmpReq, remoteKey);
//                responseList.add(responseImpl);
//            }
//        }
//
//        return responseList;
//    }
//
//    private static final int FILE_SIZE = 1024 * 1024 * 10; // 集中管控文件分割传输大小，10M
//
//    private void uploadFile(RequestImpl request, String remoteUrl, String remoteKey) throws Exception {
//        // 文件上传
//        qingzhou.deployer.App appInfo = Main.getService(Deployer.class).getApp(DeployerConstants.APP_MASTER);
//        if (appInfo != null) {
//            ModelInfo modelInfo = appInfo.getAppInfo().getModelInfo(DeployerConstants.MODEL_APP);
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
//                req.setAppName(DeployerConstants.APP_INSTANCE);
//                req.setModelName("installer");
//                req.setActionName("uploadFile");
//                req.setManageType(DeployerConstants.MANAGE_APP);
//
//                Map<String, String> parameters = new HashMap<>();
//                parameters.put("fileName", fileName);
//                parameters.put("fileBytes", Main.getService(CryptoService.class).getBase64Coder().encode(bytes));
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
//    private ResponseImpl sendReq(String url, Request request, String remoteKey) throws Exception {
//        HttpURLConnection connection = null;
//        try {
//            Json jsonService = Main.getService(Json.class);
//            String json = jsonService.toJson(request);
//
//            Cipher cipher;
//            try {
//                cipher = Main.getService(CryptoService.class).getCipher(remoteKey);
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
//}
