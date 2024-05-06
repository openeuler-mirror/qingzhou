package qingzhou.agent.impl;

import qingzhou.agent.AgentService;
import qingzhou.api.Response;
import qingzhou.config.Agent;
import qingzhou.config.ConfigService;
import qingzhou.console.RequestImpl;
import qingzhou.console.ResponseImpl;
import qingzhou.deployer.App;
import qingzhou.deployer.Deployer;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.Service;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.crypto.CryptoServiceFactory;
import qingzhou.engine.util.crypto.KeyCipher;
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
    private Json json;
    @Service
    private Http http;
    @Service
    private Logger logger;
    @Service
    private Deployer deployer;

    private String path;
    private HttpServer server;
    private AgentService agentService;

    @Override
    public void start(ModuleContext moduleContext) throws Exception {
        Agent agent = configService.getModule().getAgent();
        agentService = buildAgentService(agent);
        moduleContext.registerService(AgentService.class, agentService);

        if (!agent.isEnabled()) return;

        path = "/";
        server = http.buildHttpServer();
        server.start(agentService.getAgentHost(), agentService.getAgentPort(), 200);
        HttpContext context = server.createContext(path);
        context.setHandler(exchange -> {
            try {
                byte[] result;
                try (InputStream inputStream = exchange.getRequestBody()) {
                    result = process(inputStream);
                    exchange.setStatus(200);
                } catch (Exception e) {
                    result = Utils.stackTraceToString(e.getStackTrace()).getBytes(StandardCharsets.UTF_8);
                    exchange.setStatus(500);
                }

                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(result);
                }
            } finally {
                exchange.close();
            }
        });

        String serverUrl = "http://" + agentService.getAgentHost() + ":" + agentService.getAgentPort() + path + context.getPath();
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
        Utils.copyStream(in, bos);

        // 1. 获得请求的数据
        byte[] requestData = bos.toByteArray();

        // 2. 数据解密，带认证
        String remoteKey = agentService.getAgentKey();
        KeyCipher keyCipher = CryptoServiceFactory.getInstance().getKeyCipher(remoteKey);
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

    private AgentService buildAgentService(Agent agent) {
        String agentHost = agent.getHost();
        if (agentHost == null || agentHost.isEmpty()) {
            agentHost = "0.0.0.0";
        }
        String finalAgentHost = agentHost;
        int finalAgentPort = agent.getPort();

        return new AgentService() {
            private String agentKey;

            @Override
            public String getAgentKey() {
                if (agentKey == null) {
                    agentKey = agent.getKey() == null || agent.getKey().isEmpty()
                            ? CryptoServiceFactory.getInstance().generateKey()
                            : agent.getKey();
                }
                return agentKey;
            }

            @Override
            public String getAgentHost() {
                return finalAgentHost;
            }

            @Override
            public int getAgentPort() {
                return finalAgentPort;
            }
        };
    }
}
