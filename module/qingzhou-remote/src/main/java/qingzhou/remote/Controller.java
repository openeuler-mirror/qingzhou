package qingzhou.remote;

import qingzhou.api.Response;
import qingzhou.bootstrap.main.FrameworkContext;
import qingzhou.bootstrap.main.ModuleLoader;
import qingzhou.framework.app.App;
import qingzhou.framework.app.AppManager;
import qingzhou.framework.app.RequestImpl;
import qingzhou.framework.app.ResponseImpl;
import qingzhou.framework.config.Config;
import qingzhou.framework.crypto.CryptoService;
import qingzhou.framework.crypto.KeyCipher;
import qingzhou.framework.crypto.KeyPairCipher;
import qingzhou.framework.logger.Logger;
import qingzhou.framework.serializer.Serializer;
import qingzhou.framework.util.ExceptionUtil;
import qingzhou.framework.util.IPUtil;
import qingzhou.framework.util.StreamUtil;
import qingzhou.framework.util.StringUtil;
import qingzhou.framework.util.pattern.Process;
import qingzhou.framework.util.pattern.ProcessSequence;
import qingzhou.remote.net.http.HttpRoute;
import qingzhou.remote.net.http.HttpServer;
import qingzhou.remote.net.http.HttpServerServiceImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Controller implements ModuleLoader {
    private Config config;
    private Logger logger;
    private CryptoService cryptoService;
    private Serializer serializer;
    private AppManager appManager;

    private ProcessSequence sequence;
    private Map<String, String> remoteConfig;
    private String remoteHost;
    private int remotePort;

    @Override
    public void start(FrameworkContext frameworkContext) throws Exception {
        config = frameworkContext.getServiceManager().getService(Config.class);
        logger = frameworkContext.getServiceManager().getService(Logger.class);
        cryptoService = frameworkContext.getServiceManager().getService(CryptoService.class);
        serializer = frameworkContext.getServiceManager().getService(Serializer.class);
        appManager = frameworkContext.getServiceManager().getService(AppManager.class);

        remoteConfig = config.getConfig("//remote");
        if (!Boolean.parseBoolean(remoteConfig.get("enabled"))) return;

        sequence = new ProcessSequence(
                new StartServer(),
                new RegisterToMaster()
        );
        sequence.exec();
    }

    @Override
    public void stop(FrameworkContext frameworkContext) {
        if (sequence != null) {
            sequence.undo();
        }
    }

    private class StartServer implements Process {
        private HttpServer server;
        private String path;

        @Override
        public void exec() {
            path = "/";
            remoteHost = remoteConfig.get("host");
            remotePort = Integer.parseInt(remoteConfig.get("port"));
            server = new HttpServerServiceImpl().createHttpServer(
                    remoteHost,
                    remotePort,
                    200);
            server.addContext(new HttpRoute(path), (request, response) -> {
                byte[] result;
                try {
                    result = process(new ByteArrayInputStream(response.getContent().getBytes()));
                } catch (Exception e) {
                    result = ExceptionUtil.stackTrace(e).getBytes(StandardCharsets.UTF_8);
                }
                response.setContent(new String(result, StandardCharsets.UTF_8));
            });
            server.start();

            logger.info("The remote service is started on the port: " + remotePort);
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
            StreamUtil.copyStream(in, bos);
            // 获得请求的数据
            byte[] requestData = bos.toByteArray();

            // 数据解密，附带认证能力
            String remoteKey = config.getKey(Config.remoteKeyName);
            KeyCipher keyCipher = cryptoService.getKeyCipher(remoteKey);
            byte[] decryptedData = keyCipher.decrypt(requestData);

            // 数据转为请求对象
            RequestImpl request = serializer.deserialize(decryptedData, RequestImpl.class);

            // 处理数据对象，得到返回数据对象
            Response response = new ResponseImpl();
            App app = appManager.getApp(request.getAppName());
            app.invoke(request, response);

            // 返回数据对象转为数据
            byte[] responseData = serializer.serialize(response);
            // 返回数据加密

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
                Map<String, String> map = new HashMap<>();
                map.put("nodeIp", StringUtil.notBlank(remoteHost) ? remoteHost : String.join(",", IPUtil.getLocalIps()));
                map.put("nodePort", String.valueOf(remotePort));
                map.put("apps", String.join(",", appManager.getApps()));
                try {
                    // 获取master公钥，计算堆成密钥
                    KeyPairCipher keyPairCipher = cryptoService.getKeyPairCipher(master.get("publicKey"), null);
//                    String key = cryptoService.getKeyManager().getKeyOrElseInit(null, "", null);
//                    map.put("key", key);
                    // todo seqHttp 的参数需要简化
                    HttpClient.seqHttp(master.get("url"), map, keyPairCipher::encryptWithPublicKey);
                } catch (Exception e) {
                    logger.warn("An exception occurred during the registration process", e);
                }
            });
        }
    }
}
