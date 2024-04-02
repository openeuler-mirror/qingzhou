package qingzhou.remote;

import qingzhou.api.Response;
import qingzhou.bootstrap.main.FrameworkContext;
import qingzhou.bootstrap.main.Module;
import qingzhou.framework.app.AppInfo;
import qingzhou.framework.app.AppManager;
import qingzhou.framework.config.Config;
import qingzhou.framework.console.RequestImpl;
import qingzhou.framework.console.ResponseImpl;
import qingzhou.framework.crypto.CryptoService;
import qingzhou.framework.crypto.KeyCipher;
import qingzhou.framework.crypto.KeyPairCipher;
import qingzhou.framework.logger.Logger;
import qingzhou.framework.serializer.Serializer;
import qingzhou.framework.util.FileUtil;
import qingzhou.framework.util.IPUtil;
import qingzhou.framework.util.StringUtil;
import qingzhou.framework.util.pattern.Process;
import qingzhou.framework.util.pattern.ProcessSequence;
import qingzhou.remote.http.HttpContext;
import qingzhou.remote.http.HttpServer;
import qingzhou.remote.http.sun.HttpServerImpl;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Controller implements Module {
    private ProcessSequence sequence;

    private Map<String, String> remoteConfig;
    private String remoteHost;
    private int remotePort;
    private FrameworkContext frameworkContext;
    private Config config;

    @Override
    public void start(FrameworkContext frameworkContext) throws Exception {
        if (frameworkContext.isMaster()) return;

        this.frameworkContext = frameworkContext;

        config = frameworkContext.getService(Config.class);
        remoteConfig = config.getConfig("//remote");
        if (!Boolean.parseBoolean(remoteConfig.get("enabled"))) return;

        sequence = new ProcessSequence(
                new StartServer(),
                new RegisterToMaster()
        );
        sequence.exec();
    }

    @Override
    public void stop() {
        if (sequence != null) {
            sequence.undo();
        }
    }

    private class StartServer implements Process {
        private HttpServer server;
        private String path;

        @Override
        public void exec() throws Exception {
            path = "/";
            remoteHost = remoteConfig.get("host");
            if (StringUtil.isBlank(remoteHost)) {
                remoteHost = "0.0.0.0";
            }
            remotePort = Integer.parseInt(remoteConfig.get("port"));
            server = new HttpServerImpl();
            server.start(remoteHost, remotePort, 200);
            HttpContext context = server.createContext(path);
            context.setHandler(exchange -> {
                InputStream inputStream = exchange.getRequestBody();
                try {
                    byte[] result;
                    try {
                        result = process(inputStream);
                        inputStream.close();
                        exchange.setStatus(200);
                    } catch (Exception e) {
                        result = StringUtil.convertStackTrace(e.getStackTrace()).getBytes(StandardCharsets.UTF_8);
                        exchange.setStatus(500);
                    }
                    OutputStream outputStream = exchange.getResponseBody();
                    outputStream.write(result);
                    outputStream.close();
                } finally {
                    exchange.close();
                }
            });

            String serverUrl = "http://" + remoteHost + ":" + remotePort + path + context.getPath();
            frameworkContext.getService(Logger.class).info("The remote service is started: " + serverUrl);
        }

        @Override
        public void undo() {
            if (server != null) {
                server.removeContext(path);
                server.stop(5000);
            }
        }

        private byte[] process(InputStream in) throws Exception {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(in.available());
            FileUtil.copyStream(in, bos);

            // 1. 获得请求的数据
            byte[] requestData = bos.toByteArray();

            String remoteKey = config.getKey(Config.remoteKeyName);
            CryptoService cryptoService = frameworkContext.getService(CryptoService.class);
            KeyCipher keyCipher = cryptoService.getKeyCipher(remoteKey);

            // 2. 数据解密，附带认证能力
            byte[] decryptedData = keyCipher.decrypt(requestData);

            // 3. 处理请求
            Serializer serializer = frameworkContext.getService(Serializer.class);
            RequestImpl request = serializer.deserialize(decryptedData, RequestImpl.class);
            Response response = new ResponseImpl();
            AppManager appManager = frameworkContext.getService(AppManager.class);
            AppInfo appInfo = appManager.getApp(request.getAppName());
            appInfo.invoke(request, response);

            // 4. 响应数据
            byte[] responseData = serializer.serialize(response);

            // 5. 数据加密，返回到客户端
            return keyCipher.encrypt(responseData);
        }
    }

    private class RegisterToMaster implements Process {
        // 定时器设计目的：解决 master 未启动或者宕机重启等引起的注册失效问题
        private final Timer timer = new Timer();

        @Override
        public void exec() {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    register();
                }
            }, 2000, 1000 * 60 * 2);
        }

        @Override
        public void undo() {
            timer.cancel();
        }

        private void register() {
            List<Map<String, String>> masters = config.getConfigList("//master");

            masters.forEach(master -> {
                try {
                    Map<String, String> map = new HashMap<>();
                    map.put("nodeIp", StringUtil.notBlank(remoteHost) ? remoteHost : String.join(",", IPUtil.getLocalIps()));
                    map.put("nodePort", String.valueOf(remotePort));
                    map.put("apps", String.join(",", frameworkContext.getService(AppManager.class).getApps()));
                    // 获取master公钥，计算堆成密钥
                    CryptoService cryptoService = frameworkContext.getService(CryptoService.class);
                    String remoteKey = config.getKey(Config.remoteKeyName);
                    if (StringUtil.isBlank(remoteKey)) {
                        remoteKey = cryptoService.generateKey();
                        config.writeKey(Config.remoteKeyName, remoteKey);
                    }
                    KeyPairCipher keyPairCipher = cryptoService.getKeyPairCipher(master.get(Config.remotePublicKeyName), null);
                    map.put("key", keyPairCipher.encryptWithPublicKey(remoteKey));

                    HttpClient.seqHttp(master.get("url"), map);
                } catch (Throwable e) {
                    frameworkContext.getService(Logger.class).warn("An exception occurred during the registration process", e);
                }
            });
        }
    }
}
