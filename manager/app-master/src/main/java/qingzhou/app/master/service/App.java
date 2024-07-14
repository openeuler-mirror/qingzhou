package qingzhou.app.master.service;

import qingzhou.api.*;
import qingzhou.api.type.Createable;
import qingzhou.api.type.Deletable;
import qingzhou.api.type.Editable;
import qingzhou.api.type.Listable;
import qingzhou.app.master.MasterApp;
import qingzhou.config.Config;
import qingzhou.config.Security;
import qingzhou.console.RequestImpl;
import qingzhou.console.ResponseImpl;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.KeyCipher;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.DeployerConstants;
import qingzhou.engine.util.Utils;
import qingzhou.json.Json;
import qingzhou.registry.*;

import javax.net.ssl.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;

@Model(code = DeployerConstants.MASTER_APP_APP_MODEL_NAME, icon = "cube-alt",
        menu = "Service", order = 1,
        name = {"应用", "en:App"},
        info = {"应用。",
                "en:App Management."})
public class App extends ModelBase implements Createable {
    @ModelField(
            list = true, editable = false, createable = false,
            name = {"名称", "en:Name"},
            info = {"应用名称。", "en:App Name"})
    public String id;

    @ModelField(
            type = FieldType.bool,
            name = {"使用上传", "en:Enable Upload"},
            info = {"安装的应用可以从客户端上传，也可以从服务器端指定的位置读取。",
                    "en:The installed app can be uploaded from the client or read from a location specified on the server side."})
    public boolean appFrom = false;

    @ModelField(
            list = true, show = "appFrom=false",
            name = {"应用位置", "en:Application File"},
            info = {"服务器上应用程序的位置，通常是应用的程序包，注：须为 *.jar, *.zip 类型的文件或目录。",
                    "en:The location of the application on the server, usually the app package, Note: Must be a *.jar, *.zip file or directory."})
    public String filename;

    @ModelField(
            type = FieldType.file, show = "appFrom=true",
            name = {"上传应用", "en:Upload Application"},
            info = {"上传一个应用文件到服务器，文件须是 *.jar 或 *.zip 类型的 Qingzhou 应用文件，否则可能会导致安装失败。",
                    "en:Upload an application file to the server, the file must be a *.jar type qingzhou application file, otherwise the installation may fail."})
    public String fromUpload;

    @ModelField(
            //type = FieldType.multiselect,
            list = true, //refModel = Instance.class,
            name = {"实例", "en:Instance"},
            info = {"选择安装应用的实例。", "en:Select the instance where you want to install the application."})
    public String instances;

    @Override
    public void start() {
        appContext.addI18n("app.id.not.exist", new String[]{"应用文件不存在", "en:The app file does not exist"});
        appContext.addI18n("app.type.unknown", new String[]{"未知的应用类型", "en:Unknown app type"});

        actionFilters.add((request, response) -> {
            if (request.getId().equals(DeployerConstants.MASTER_APP_NAME)
                    || request.getId().equals(DeployerConstants.INSTANCE_APP_NAME)) {
                if (Editable.ACTION_NAME_UPDATE.equals(request.getAction())
                        || Deletable.ACTION_NAME_DELETE.equals(request.getAction())) {
                    return appContext.getI18n(request.getLang(), "validator.master.system");
                }
            }
            return null;
        });
    }

    @ModelAction(
            name = {"查看", "en:Show"},
            info = {"查看该组件的相关信息。", "en:View the information of this model."})
    public void show(Request request, Response response) throws Exception {
        Map<String, String> appMap = new HashMap<>();
        String id = request.getId();
        qingzhou.deployer.App app = MasterApp.getService(Deployer.class).getApp(id);
        if (app != null) {
            appMap.put("id", id);
            appMap.put("instances", DeployerConstants.MASTER_APP_DEFAULT_INSTANCE_ID);
            appMap.put("filename", ""); // ToDo
            response.addData(appMap);
            return;
        }

        try {
            Registry registry = MasterApp.getService(Registry.class);
            Collection<String> allInstanceIds = registry.getAllInstanceId();
            // 处理远程实例的应用信息
            for (String instanceId : allInstanceIds) {
                InstanceInfo instanceInfo = registry.getInstanceInfo(instanceId);
                AppInfo[] appInfos = instanceInfo.getAppInfos();
                for (AppInfo appInfo : appInfos) {
                    if (id.equals(appInfo.getName())) {
                        appMap.put("id", appInfo.getName());
                        appMap.put("instances", instanceId);
                        appMap.put("filename", ""); // ToDo
                        response.addData(appMap);
                        break;
                    }
                }
                if (!appMap.isEmpty()) {
                    break;
                }
            }
        } catch (Exception ignored) {
        }
    }

    @ModelAction(
            name = {"编辑", "en:Edit"},
            info = {"获得可编辑的数据或界面。", "en:Get editable data or interfaces."})
    public void edit(Request request, Response response) throws Exception {
        show(request, response);
    }

    @ModelAction(
            name = {"更新", "en:Update"},
            info = {"更新这个模块的配置信息。", "en:Update the configuration information for this module."})
    public void update(Request request, Response response) throws Exception {
        try {
            delete(request, response);
            add(request, response);
        } finally {
            ((RequestImpl) request).setManageType(DeployerConstants.MANAGE_TYPE_APP);
            ((RequestImpl) request).setAppName(DeployerConstants.MASTER_APP_NAME);
            ((RequestImpl) request).setModelName(DeployerConstants.MASTER_APP_APP_MODEL_NAME);
            ((RequestImpl) request).setActionName(Editable.ACTION_NAME_UPDATE);
        }
    }

    @ModelAction(
            name = {"列表", "en:List"},
            info = {"展示该类型的所有组件数据或界面。", "en:Show all component data or interfaces of this type."})
    public void list(Request request, Response response) throws Exception {
        int pageNum = 1;
        try {
            pageNum = Integer.parseInt(request.getParameter(Listable.PARAMETER_PAGE_NUM));
        } catch (NumberFormatException ignored) {
        }

        int pageSize = response.getPageSize();
        if (pageSize == -1) {
            pageSize = 10;
        }

        Deployer deployer = MasterApp.getService(Deployer.class);
        Collection<String> localAppNames = deployer.getAllApp();
        Map<String, Set<String>> uniqueApps = new HashMap<>();

        // 处理本地应用名称
        for (String appName : localAppNames) {
            if (DeployerConstants.MASTER_APP_NAME.equals(appName) || DeployerConstants.INSTANCE_APP_NAME.equals(appName)) {
                continue;
            }
            uniqueApps.computeIfAbsent(appName, k -> new HashSet<>()).add(DeployerConstants.MASTER_APP_DEFAULT_INSTANCE_ID);
        }

        try {
            // 处理远程实例的应用信息
            Registry registry = MasterApp.getService(Registry.class);
            for (String instanceId : registry.getAllInstanceId()) {
                InstanceInfo instanceInfo = registry.getInstanceInfo(instanceId);
                for (AppInfo appInfo : instanceInfo.getAppInfos()) {
                    String appName = appInfo.getName();
                    uniqueApps.computeIfAbsent(appName, k -> new HashSet<>()).add(instanceId);
                }
            }
        } catch (Exception ignored) {
        }

        List<Map<String, String>> finalAppList = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : uniqueApps.entrySet()) {
            String appName = entry.getKey();
            Set<String> instances = entry.getValue();
            Map<String, String> appMap = new HashMap<>();
            appMap.put("id", appName);
            appMap.put("instances", String.join(",", instances));
            appMap.put("filename", !(DeployerConstants.INSTANCE_APP_NAME.equals(appName) || DeployerConstants.MASTER_APP_NAME.equals(appName)) ? "apps/" + appName : "");
            finalAppList.add(appMap);
        }

        int totalSize = finalAppList.size();
        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalSize);

        List<Map<String, String>> pagedApps = finalAppList.subList(startIndex, endIndex);
        for (Map<String, String> app : pagedApps) {
            response.addData(app);
        }

        response.setTotalSize(totalSize);
        response.setPageSize(pageSize);
        response.setPageNum(pageNum);
    }

    @ModelAction(
            name = {"部署", "en:Deploy"},
            info = {"部署应用到本地实例。", "en:Deploy the app to an on-premises instance."})
    public void create(Request request, Response response) throws Exception {
        response.addModelData(new App());
    }

    @ModelAction(
            name = {"安装", "en:Install"},
            info = {"按配置要求安装应用到指定的实例。", "en:Install the app to the specified instance as required."})
    public void add(Request request, Response response) throws Exception {
        String[] instances = request.getParameter("instances") != null
                ? request.getParameter("instances").split(",")
                : new String[0];
        ((RequestImpl) request).setAppName(DeployerConstants.INSTANCE_APP_NAME);
        ((RequestImpl) request).setModelName("appinstaller");
        ((RequestImpl) request).setActionName("installApp");
        try {
            Registry registry = MasterApp.getService(Registry.class);
            Security security = MasterApp.getService(Config.class).getConsole().getSecurity();
            for (String instance : instances) {
                try {
                    if (DeployerConstants.MASTER_APP_DEFAULT_INSTANCE_ID.equals(instance)) { // 安装到本地节点
                        MasterApp.getService(Deployer.class).getApp(DeployerConstants.INSTANCE_APP_NAME).invokeDirectly(request, response);
                    } else {
                        InstanceInfo instanceInfo = registry.getInstanceInfo(instance);
                        String remoteUrl = String.format("http://%s:%s", instanceInfo.getHost(), instanceInfo.getPort());
                        String remoteKey = appContext.getService(CryptoService.class).getKeyPairCipher(security.getPublicKey(), security.getPrivateKey()).decryptWithPrivateKey(instanceInfo.getKey());

                        uploadFile((RequestImpl) request, remoteUrl, remoteKey);

                        ResponseImpl responseImpl = sendReq(remoteUrl, request, remoteKey);
                        if (!responseImpl.isSuccess()) {
                            System.out.println(responseImpl.getMsg());
                        }
                    }
                } catch (Exception e) { // todo 部分失败，如何显示到页面？
                    response.setSuccess(false);
                    if (e instanceof InvocationTargetException) {
                        response.setMsg(((InvocationTargetException) e).getTargetException().getMessage());
                    } else {
                        response.setMsg(e.getMessage());
                    }
                }
            }
        } finally {
            ((RequestImpl) request).setAppName(DeployerConstants.MASTER_APP_NAME);
            ((RequestImpl) request).setModelName(DeployerConstants.MASTER_APP_APP_MODEL_NAME);
            ((RequestImpl) request).setActionName(Createable.ACTION_NAME_ADD);
        }
    }

    private static final int FILE_SIZE = 1024 * 1024 * 10; // 集中管控文件分割传输大小，10M

    private void uploadFile(RequestImpl request, String remoteUrl, String remoteKey) throws Exception {
        // 文件上传
        qingzhou.deployer.App appInfo = MasterApp.getService(Deployer.class).getApp(DeployerConstants.MASTER_APP_NAME);
        if (appInfo != null) {
            ModelInfo modelInfo = appInfo.getAppInfo().getModelInfo(DeployerConstants.MASTER_APP_APP_MODEL_NAME);
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
            if (size > FILE_SIZE) {
                readSize = FILE_SIZE;
                count = (int) (size / readSize);
                count = size % readSize == 0 ? count : count + 1; // 文件分片数
            }
            byte[] bytes = new byte[readSize];
            in = Files.newInputStream(tempFile.toPath());
            bis = new BufferedInputStream(in);
            int len;
            for (int i = 0; i < count; i++) {
                len = bis.read(bytes);
                RequestImpl req = new RequestImpl();
                req.setAppName(DeployerConstants.INSTANCE_APP_NAME);
                req.setModelName("appinstaller");
                req.setActionName("uploadFile");
                req.setManageType(DeployerConstants.MANAGE_TYPE_APP);

                Map<String, String> parameters = new HashMap<>();
                parameters.put("fileName", fileName);
                parameters.put("fileBytes", appContext.getService(CryptoService.class).getHexCoder().bytesToHex(bytes));
                parameters.put("len", String.valueOf(len));
                parameters.put("isStart", String.valueOf(i == 0));
                parameters.put("isEnd", String.valueOf(i == count - 1));
                parameters.put("timestamp", timestamp);
                req.setParameters(parameters);

                ResponseImpl response = sendReq(remoteUrl, req, remoteKey);
                if (response.isSuccess()) {
                    List<Map<String, String>> dataList = response.getDataList();
                    if (!dataList.isEmpty()) {
                        return dataList.get(0).get("fileName");
                    }
                } else {
                    break;
                }
            }
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

    @ModelAction(
            show = "id!=master&id!=instance",
            name = {"管理", "en:Manage"}, order = 1,
            info = {"转到此应用的管理页面。", "en:Go to the administration page for this app."})
    public void manage(Request request, Response response) throws Exception {
    }

    @ModelAction(
            ajax = true,
            batch = true, order = 2, show = "id!=master&id!=instance",
            name = {"卸载", "en:Uninstall"},
            info = {"卸载应用，只能卸载本地实例部署的应用。注：请谨慎操作，删除后不可恢复。",
                    "en:If you uninstall an application, you can only uninstall an application deployed on a local instance. Note: Please operate with caution, it cannot be recovered after deletion."})
    public void delete(Request request, Response response) throws Exception {
        String appName = request.getId();
        Deployer deployer = MasterApp.getService(Deployer.class);
        qingzhou.deployer.App app = deployer.getApp(appName);

        ((RequestImpl) request).setManageType(DeployerConstants.MANAGE_TYPE_INSTANCE);
        ((RequestImpl) request).setAppName(DeployerConstants.INSTANCE_APP_NAME);
        ((RequestImpl) request).setModelName("appinstaller");
        ((RequestImpl) request).setActionName("unInstallApp");
        try {
            if (app != null) {
                deployer.getApp(DeployerConstants.INSTANCE_APP_NAME).invokeDirectly(request, response);
            }

            // 卸载远程实例
            Registry registry = MasterApp.getService(Registry.class);
            AppInfo appInfo = registry.getAppInfo(appName);
            if (appInfo != null) {
                Security security = MasterApp.getService(Config.class).getConsole().getSecurity();
                for (String instanceId : registry.getAllInstanceId()) {
                    InstanceInfo instanceInfo = registry.getInstanceInfo(instanceId);
                    for (AppInfo info : instanceInfo.getAppInfos()) {
                        if (appName.equals(info.getName())) {
                            ((RequestImpl) request).setAppName(instanceId);
                            String remoteUrl = String.format("http://%s:%s", instanceInfo.getHost(), instanceInfo.getPort());
                            String remoteKey = appContext.getService(CryptoService.class).getKeyPairCipher(security.getPublicKey(), security.getPrivateKey()).decryptWithPrivateKey(instanceInfo.getKey());
                            ResponseImpl responseImpl = sendReq(remoteUrl, request, remoteKey);
                            if (!responseImpl.isSuccess()) {
                                System.out.println(responseImpl.getMsg());
                            }
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (e.getMessage().contains("App not found:")) { // todo 部分失败，如何显示到页面？
                return;
            }
            response.setSuccess(false);
            response.setMsg(e.getMessage());
        } finally {
            ((RequestImpl) request).setManageType(DeployerConstants.MANAGE_TYPE_APP);
            ((RequestImpl) request).setAppName(DeployerConstants.MASTER_APP_NAME);
            ((RequestImpl) request).setModelName(DeployerConstants.MASTER_APP_APP_MODEL_NAME);
            ((RequestImpl) request).setActionName(Deletable.ACTION_NAME_DELETE);
        }
    }

    private ResponseImpl sendReq(String url, Request request, String remoteKey) throws Exception {
        HttpURLConnection connection = null;
        try {
            Json jsonService = appContext.getService(Json.class);
            String json = jsonService.toJson(request);

            KeyCipher cipher;
            try {
                cipher = appContext.getService(CryptoService.class).getKeyCipher(remoteKey);
            } catch (Exception ignored) {
                throw new RuntimeException("remoteKey error");
            }
            byte[] encrypt = cipher.encrypt(json.getBytes(StandardCharsets.UTF_8));

            connection = buildConnection(url);
            try (OutputStream outStream = connection.getOutputStream()) {
                outStream.write(encrypt);
                outStream.flush();
            }

            try (InputStream inputStream = connection.getInputStream();
                 ByteArrayOutputStream bos = new ByteArrayOutputStream(inputStream.available())) {
                Utils.copyStream(inputStream, bos);
                byte[] decryptedData = cipher.decrypt(bos.toByteArray());
                return jsonService.fromJson(new String(decryptedData, StandardCharsets.UTF_8), ResponseImpl.class);
            }
        } catch (Exception e) {
            if (e instanceof RuntimeException || e instanceof FileNotFoundException) {
                throw e;
            } else {
                throw new RuntimeException(String.format("Remote server [%s] request error: %s.",
                        url,
                        e.getMessage()));
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static SSLSocketFactory ssf;
    public static final X509TrustManager TRUST_ALL_MANAGER = new X509TrustManagerInternal();

    static class X509TrustManagerInternal implements X509TrustManager {
        //返回受信任的X509证书数组。
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        //该方法检查服务器的证书，若不信任该证书同样抛出异常。通过自己实现该方法，可以使之信任我们指定的任何证书。
        //在实现该方法时，也可以简单的不做任何处理，即一个空的函数体，由于不会抛出异常，它就会信任任何证书。
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        //该方法检查客户端的证书，若不信任该证书则抛出异常。由于我们不需要对客户端进行认证，
        //因此我们只需要执行默认的信任管理器的这个方法。JSSE中，默认的信任管理器类为TrustManager。
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }
    }

    private static HttpURLConnection buildConnection(String url) throws Exception {
        HttpURLConnection conn;
        URL http = new URL(url);
        if (url.startsWith("https:")) {
            if (ssf == null) {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(new KeyManager[0], new TrustManager[]{TRUST_ALL_MANAGER}, new SecureRandom());
                ssf = sslContext.getSocketFactory();
            }
            HttpsURLConnection httpsConn = (HttpsURLConnection) http.openConnection();
            httpsConn.setSSLSocketFactory(ssf);
            httpsConn.setHostnameVerifier((hostname, session) -> true);
            conn = httpsConn;
        } else {
            conn = (HttpURLConnection) http.openConnection();
        }

        setConnectionProperties(conn);

        return conn;
    }

    private static void setConnectionProperties(HttpURLConnection conn) throws Exception {
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setConnectTimeout(60000);
        conn.setRequestProperty("Connection", "close");
        conn.setRequestProperty("Charset", "UTF-8");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");// 设置文件类型
        conn.setRequestProperty("accept", "*/*");// 设置接收类型否则返回415错误
        conn.setInstanceFollowRedirects(false);// 不处理重定向，否则“动态密钥需要刷新”提示信息收不到。。。
    }
}
