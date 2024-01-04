package qingzhou.remote.impl;

import com.sun.net.httpserver.HttpServer;
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
import qingzhou.framework.util.ServerUtil;
import qingzhou.framework.util.StreamUtil;
import qingzhou.logger.Logger;
import qingzhou.logger.LoggerService;
import qingzhou.serializer.Serializer;
import qingzhou.serializer.SerializerService;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class Controller implements BundleActivator {
    private ServiceReference<FrameworkContext> serviceReference;
    private FrameworkContext frameworkContext;

    private HttpServer server;
    private String path;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        serviceReference = bundleContext.getServiceReference(FrameworkContext.class);
        frameworkContext = bundleContext.getService(serviceReference);

        int port = 7000;// todo 可配置
        path = "/";
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(path, httpExchange -> {
            byte[] result;
            int responseCode;
            try {
                result = process(httpExchange.getRequestBody());
                responseCode = 200;
            } catch (Exception e) {
                result = ExceptionUtil.stackTrace(e).getBytes(StandardCharsets.UTF_8);
                responseCode = 500;
            }
            httpExchange.sendResponseHeaders(responseCode, result.length);
            OutputStream os = httpExchange.getResponseBody();
            os.write(result);
            os.close();
        });
        server.start();

        Logger logger = frameworkContext.getService(LoggerService.class).getLogger();
        logger.info("The remote service is started on the port: " + port);
    }

    @Override
    public void stop(BundleContext bundleContext) {
        server.removeContext(path);
        server.stop(1);

        bundleContext.ungetService(serviceReference);
    }

    private byte[] process(InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(in.available());
        StreamUtil.copyStream(in, bos);
        // 获得请求的数据
        byte[] requestData = bos.toByteArray();

        // 数据解密，附带认证能力
        CryptoService cryptoService = frameworkContext.getService(CryptoService.class);
        String remoteKey = cryptoService.getKeyManager().getKeyOrElseInit(ServerUtil.getSecureFile(ServerUtil.getDomain()), ServerUtil.remoteKeyName, null);
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
}
