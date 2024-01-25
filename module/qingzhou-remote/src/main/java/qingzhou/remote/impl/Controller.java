package qingzhou.remote.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import qingzhou.console.ConsoleConstants;
import qingzhou.console.RequestImpl;
import qingzhou.console.ResponseImpl;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.PasswordCipher;
import qingzhou.crypto.PublicKeyCipher;
import qingzhou.framework.App;
import qingzhou.framework.AppManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.Logger;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;
import qingzhou.framework.pattern.Process;
import qingzhou.framework.pattern.ProcessSequence;
import qingzhou.framework.util.ExceptionUtil;
import qingzhou.framework.util.FileUtil;
import qingzhou.framework.util.StreamUtil;
import qingzhou.remote.impl.net.HttpRoute;
import qingzhou.remote.impl.net.HttpServer;
import qingzhou.remote.impl.net.impl.tinyserver.HttpServerServiceImpl;
import qingzhou.serializer.Serializer;
import qingzhou.serializer.SerializerService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class Controller implements BundleActivator {
    private ServiceReference<FrameworkContext> serviceReference;
    private FrameworkContext frameworkContext;
    private ProcessSequence sequence;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        serviceReference = bundleContext.getServiceReference(FrameworkContext.class);
        frameworkContext = bundleContext.getService(serviceReference);
        if (frameworkContext.isMaster()) return;

        sequence = new ProcessSequence(
                new StartServer(),
                new RegisterToMaster()
        );
        sequence.exec();
    }

    @Override
    public void stop(BundleContext bundleContext) {
        bundleContext.ungetService(serviceReference);
        if (frameworkContext.isMaster()) return;

        sequence.undo();
    }

    private class StartServer implements Process {
        private HttpServer server;
        private String path;

        @Override
        public void exec() throws Exception {
            int port = 7000;// todo 可配置
            path = "/";
            server = new HttpServerServiceImpl().createHttpServer(port, 200);
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

            frameworkContext.getServiceManager().getService(Logger.class).info("The remote service is started on the port: " + port);
        }

        @Override
        public void undo() {
            server.removeContext(path);
            server.stop(5000);
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
            Set<String> apps = frameworkContext.getAppManager().getApps();
            Map<String, String> map = new HashMap<>();
            map.put("nodeIp", getIp());
            map.put("nodePort", String.valueOf(getPort()));
            map.put("apps", String.join(",", apps));
            try {
                // 获取master公钥，计算堆成密钥
                String publicKey = "";
                CryptoService cryptoService = frameworkContext.getServiceManager().getService(CryptoService.class);
                PublicKeyCipher publicKeyCipher = cryptoService.getPublicKeyCipher(publicKey, null);
                String key = cryptoService.getKeyManager().getKeyOrElseInit(null, "", null);
                map.put("key", key);

                String res = HttpClient.seqHttp(getMasterAddress(), map, publicKeyCipher::encryptWithPublicKey);
                // todo 解析 res
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String getIp() {
        return "";
    }

    public int getPort() {
        return 9999;
    }

    public String getMasterAddress() {
        return "" + ConsoleConstants.REGISTER_URI;
    }
}
