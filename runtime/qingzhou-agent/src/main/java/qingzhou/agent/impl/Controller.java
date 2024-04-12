package qingzhou.agent.impl;

import qingzhou.config.Config;
import qingzhou.config.Remote;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.KeyCipher;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.StringUtil;
import qingzhou.http.HttpContext;
import qingzhou.http.HttpServer;
import qingzhou.json.Json;
import qingzhou.logger.Logger;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class Controller implements Module {
    private String path;
    private HttpServer server;

    @Override
    public void start(ModuleContext moduleContext) throws Exception {
        Config config = moduleContext.getService(Config.class);
        Remote remote = config.getRemote();
        if (!remote.isEnabled()) return;

        path = "/";
        String remoteHost = remote.getHost();
        if (remoteHost == null || remoteHost.isEmpty()) {
            remoteHost = "0.0.0.0";
        }
        server = moduleContext.getService(HttpServer.class);
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

        Logger logger = moduleContext.getService(Logger.class);
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

        String remoteKey = config.getKey(Config.remoteKeyName);
        CryptoService cryptoService = moduleContext.getService(CryptoService.class);
        KeyCipher keyCipher = cryptoService.getKeyCipher(remoteKey);

        // 2. 数据解密，附带认证能力
        byte[] decryptedData = keyCipher.decrypt(requestData);

        // 3. 处理请求
        Json json = moduleContext.getService(Json.class);
        RequestImpl request = json.deserialize(decryptedData, RequestImpl.class);
        Response response = new ResponseImpl();
        AppManager appManager = moduleContext.getService(AppManager.class);
        AppInfo appInfo = appManager.getApp(request.getAppName());
        appInfo.invoke(request, response);

        // 4. 响应数据
        byte[] responseData = json.serialize(response);

        // 5. 数据加密，返回到客户端
        return keyCipher.encrypt(responseData);
    }
}
