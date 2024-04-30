package qingzhou.agent.impl;

import qingzhou.agent.Agent;
import qingzhou.api.Response;
import qingzhou.config.ConfigService;
import qingzhou.config.Remote;
import qingzhou.console.RequestImpl;
import qingzhou.console.ResponseImpl;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.KeyCipher;
import qingzhou.deployer.App;
import qingzhou.deployer.Deployer;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.Service;
import qingzhou.engine.util.FileUtil;
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
    private Http http;
    @Service
    private Logger logger;
    @Service
    private Deployer deployer;

    private Agent agent;
    private String path;
    private HttpServer server;

    @Override
    public void start(ModuleContext moduleContext) throws Exception {
        Remote remote = configService.getConfig().getRemote();
        agent = new AgentImpl(remote, cryptoService);
        moduleContext.registerService(Agent.class, agent);

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
            try {
                byte[] result;
                try (InputStream inputStream = exchange.getRequestBody()) {
                    result = process(inputStream);
                    exchange.setStatus(200);
                } catch (Exception e) {
                    result = convertStackTrace(e.getStackTrace()).getBytes(StandardCharsets.UTF_8);
                    exchange.setStatus(500);
                }

                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(result);
                }
            } finally {
                exchange.close();
            }
        });

        String serverUrl = "http://" + remoteHost + ":" + remote.getPort() + path + context.getPath();
        logger.info("The remote service is started: " + serverUrl);
    }

    private String convertStackTrace(StackTraceElement[] stackTrace) {
        StringBuilder msg = new StringBuilder();
        String sp = System.lineSeparator();
        for (StackTraceElement element : stackTrace) {
            msg.append("\t").append(element).append(sp);
        }
        return msg.toString();
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
        String remoteKey = agent.thisInstanceInfo().getKey();
        KeyCipher keyCipher = cryptoService.getKeyCipher(remoteKey);
        byte[] decryptedData = keyCipher.decrypt(requestData);

        // 3. 得到请求对象
        RequestImpl request = json.fromJson(new String(decryptedData, StandardCharsets.UTF_8), RequestImpl.class);

        // 4. 处理
        Response response = new ResponseImpl();
        App app = deployer.getApp(request.getApp());
        app.invoke(request, response);

        // 5. 响应数据
        byte[] responseData = json.toJson(response).getBytes(StandardCharsets.UTF_8);

        // 6. 数据加密，返回到客户端
        return keyCipher.encrypt(responseData);
    }
}
