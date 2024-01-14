package qingzhou.remote.impl;

import java.io.ByteArrayInputStream;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.PasswordCipher;
import qingzhou.framework.AppInfo;
import qingzhou.framework.AppManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;
import qingzhou.framework.console.RequestImpl;
import qingzhou.framework.console.ResponseImpl;
import qingzhou.framework.util.ExceptionUtil;
import qingzhou.framework.util.FileUtil;
import qingzhou.framework.util.StreamUtil;
import qingzhou.httpserver.HttpServer;
import qingzhou.httpserver.HttpServerService;
import qingzhou.logger.Logger;
import qingzhou.logger.LoggerService;
import qingzhou.remote.RemoteConstants;
import qingzhou.serializer.Serializer;
import qingzhou.serializer.SerializerService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import qingzhou.httpserver.HttpRoute;

public class Controller implements BundleActivator {
    private ServiceReference<FrameworkContext> serviceReference;
    private FrameworkContext frameworkContext;

    private HttpServer server;
    private String path;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        serviceReference = bundleContext.getServiceReference(FrameworkContext.class);
        frameworkContext = bundleContext.getService(serviceReference);
        Logger logger = frameworkContext.getService(LoggerService.class).getLogger();

        int port = 7000;// todo 可配置
        path = "/";
        server = frameworkContext.getService(HttpServerService.class).createHttpServer(port, 200);
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

        logger.info("The remote service is started on the port: " + port);
    }

    @Override
    public void stop(BundleContext bundleContext) {
        if (server != null) {
            server.removeContext(path);
            server.stop(5000);
        }

        bundleContext.ungetService(serviceReference);
    }

    private byte[] process(InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(in.available());
        StreamUtil.copyStream(in, bos);
        // 获得请求的数据
        byte[] requestData = bos.toByteArray();

        // 数据解密，附带认证能力
        CryptoService cryptoService = frameworkContext.getService(CryptoService.class);
        String remoteKey = cryptoService.getKeyManager().getKeyOrElseInit(getSecureFile(frameworkContext.getDomain()), RemoteConstants.REMOTE_KEY_NAME, null);
        PasswordCipher passwordCipher = cryptoService.getPasswordCipher(remoteKey);
        byte[] decryptedData = passwordCipher.decrypt(requestData);

        // 数据转为请求对象
        Serializer serializer = frameworkContext.getService(SerializerService.class).getSerializer();
        Request request = serializer.deserialize(decryptedData, RequestImpl.class);

        // 处理数据对象，得到返回数据对象
        Response response = new ResponseImpl();
        AppManager appManager = frameworkContext.getAppManager();
        AppInfo appInfo = appManager.getAppInfo(request.getAppName());
        appInfo.invokeAction(request, response);

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
