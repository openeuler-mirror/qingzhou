package qingzhou.remote.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.PasswordCipher;
import qingzhou.crypto.PublicKeyCipher;
import qingzhou.framework.App;
import qingzhou.framework.AppManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.Logger;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;
import qingzhou.framework.RequestImpl;
import qingzhou.framework.ResponseImpl;
import qingzhou.framework.pattern.Process;
import qingzhou.framework.pattern.ProcessSequence;
import qingzhou.framework.util.*;
import qingzhou.remote.impl.net.http.HttpRoute;
import qingzhou.remote.impl.net.http.HttpServer;
import qingzhou.remote.impl.net.http.HttpServerServiceImpl;
import qingzhou.serializer.Serializer;
import qingzhou.serializer.SerializerService;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Controller implements BundleActivator {
    private ServiceReference<FrameworkContext> serviceReference;
    private FrameworkContext frameworkContext;
    private ProcessSequence sequence;
    private Map<String, String> remoteConfig;
    private String remoteHost;
    private int remotePort;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        serviceReference = bundleContext.getServiceReference(FrameworkContext.class);
        frameworkContext = bundleContext.getService(serviceReference);

        remoteConfig = frameworkContext.getConfigManager().getConfig("//remote");
        if (!Boolean.parseBoolean(remoteConfig.get("enabled"))) return;

        sequence = new ProcessSequence(
                new StartServer(),
                new RegisterToMaster()
        );
        sequence.exec();
    }

    @Override
    public void stop(BundleContext bundleContext) {
        bundleContext.ungetService(serviceReference);

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
                try {
                    response.setContent(new String(result, StandardCharsets.UTF_8));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            server.start();

            frameworkContext.getServiceManager().getService(Logger.class).info("The remote service is started on the port: " + remotePort);
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
            CryptoService cryptoService = frameworkContext.getServiceManager().getService(CryptoService.class);
            String remoteKey = cryptoService.getKeyManager().getKeyOrElseInit(getSecureFile(frameworkContext.getFileManager().getDomain()), "remoteKey", null);
            PasswordCipher passwordCipher = cryptoService.getPasswordCipher(remoteKey);
            byte[] decryptedData = passwordCipher.decrypt(requestData);

            // 数据转为请求对象
            Serializer serializer = frameworkContext.getServiceManager().getService(SerializerService.class).getSerializer();
            Request request = serializer.deserialize(decryptedData, RequestImpl.class);

            // 处理数据对象，得到返回数据对象
            Response response = new ResponseImpl();
            AppManager appManager = frameworkContext.getAppManager();
            App app = appManager.getApp(request.getAppName());
            app.invoke(request, response);

            // 返回数据对象转为数据
            byte[] responseData = serializer.serialize(response);
            // 返回数据加密

            return passwordCipher.encrypt(responseData);
        }

        private File getSecureFile(File domain) throws IOException {
            File secureDir = FileUtil.newFile(domain, "data", "secure");
            FileUtil.mkdirs(secureDir);
            File secureFile = FileUtil.newFile(secureDir, "secure");
            if (!secureFile.exists()) {
                if (!secureFile.createNewFile()) {
                    throw ExceptionUtil.unexpectedException(secureFile.getPath());
                }
            }

            return secureFile;
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
            List<Map<String, String>> masters = frameworkContext.getConfigManager().getConfigList("//master");

            masters.forEach(master -> {
                Map<String, String> map = new HashMap<>();
                map.put("nodeIp", StringUtil.notBlank(remoteHost) ? remoteHost : String.join(",", IPUtil.getLocalIps()));
                map.put("nodePort", String.valueOf(remotePort));
                map.put("apps", String.join(",", frameworkContext.getAppManager().getApps()));
                try {
                    // 获取master公钥，计算堆成密钥
                    CryptoService cryptoService = frameworkContext.getServiceManager().getService(CryptoService.class);
                    PublicKeyCipher publicKeyCipher = cryptoService.getPublicKeyCipher(master.get("publicKey"), null);
//                    String key = cryptoService.getKeyManager().getKeyOrElseInit(null, "", null);
//                    map.put("key", key);
                    // todo seqHttp 的参数需要简化
                    HttpClient.seqHttp(master.get("url"), map, publicKeyCipher::encryptWithPublicKey);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
