package qingzhou.agent.impl;

import qingzhou.api.Response;
import qingzhou.config.ConfigService;
import qingzhou.config.Remote;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.KeyCipher;
import qingzhou.deployer.App;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.RequestImpl;
import qingzhou.deployer.ResponseImpl;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.Service;
import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.StringUtil;
import qingzhou.http.Http;
import qingzhou.http.HttpContext;
import qingzhou.http.HttpServer;
import qingzhou.json.Json;
import qingzhou.logger.Logger;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@Module
public class Controller implements ModuleActivator {
    @Service
    private ConfigService configService;
    @Service
    private CryptoService cryptoService;
    @Service
    private Json json;
    @Service
    private Deployer deployer;
    @Service
    private Http http;
    @Service
    private Logger logger;

    private String path;
    private HttpServer server;

    @Override
    public void start(ModuleContext moduleContext) throws Exception {
        Remote remote = configService.getConfig().getRemote();
        if (!remote.isEnabled()) return;

        path = "/";
        String remoteHost = remote.getHost();
        if (remoteHost == null || remoteHost.isEmpty()) {
            remoteHost = "0.0.0.0";
        }
        server = http.buildHttpServer();
        server.start(remoteHost, remote.getPort(), 200);
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

        String serverUrl = "http://" + remoteHost + ":" + remote.getPort() + path + context.getPath();
        logger.info("The remote service is started: " + serverUrl);
    }

    @Override
    public void stop() {
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

        // 2. 数据解密，带认证
        String remoteKey = deployer.getInstanceInfo().getKey();
        KeyCipher keyCipher = cryptoService.getKeyCipher(remoteKey);
        byte[] decryptedData = keyCipher.decrypt(requestData);

        // 3. 处理请求
        RequestImpl request = json.fromJson(new String(decryptedData, StandardCharsets.UTF_8), RequestImpl.class);
        Response response = new ResponseImpl();
        App app = deployer.getApp(request.getApp());
        app.invoke(request, response);

        // 4. 响应数据
        byte[] responseData = json.toJson(response).getBytes(StandardCharsets.UTF_8);

        // 5. 数据加密，返回到客户端
        return keyCipher.encrypt(responseData);
    }
}
