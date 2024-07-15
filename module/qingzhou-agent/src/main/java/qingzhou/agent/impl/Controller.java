package qingzhou.agent.impl;

import qingzhou.agent.AgentService;
import qingzhou.config.Agent;
import qingzhou.config.Config;
import qingzhou.console.RequestImpl;
import qingzhou.console.ResponseImpl;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.KeyCipher;
import qingzhou.deployer.App;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.DeployerConstants;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.Service;
import qingzhou.engine.util.Utils;
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
    private Config config;
    @Service
    private Json json;
    @Service
    private Http http;
    @Service
    private Logger logger;
    @Service
    private Deployer deployer;
    @Service
    private CryptoService cryptoService;

    private String path;
    private HttpServer server;
    private AgentService agentService;

    @Override
    public void start(ModuleContext moduleContext) throws Exception {
        Agent agent = config.getAgent();
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

        String serverUrl = "http://" + agentService.getAgentHost() + ":" + agentService.getAgentPort() + context.getPath();
        logger.info("The remote service is started: " + serverUrl);
    }

    @Override
    public void stop() {
        if (server != null) {
            server.removeContext(path);
            server.stop(0);
        }
    }

    private byte[] process(InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(in.available());
        Utils.copyStream(in, bos);

        // 1. 获得请求的数据
        byte[] requestData = bos.toByteArray();

        // 2. 数据解密，带认证
        String remoteKey = agentService.getAgentKey();
        KeyCipher keyCipher = cryptoService.getKeyCipher(remoteKey);
        byte[] decryptedData = keyCipher.decrypt(requestData);

        // 3. 得到请求对象
        RequestImpl request = json.fromJson(new String(decryptedData, StandardCharsets.UTF_8), RequestImpl.class);

        // 4. 处理
        ResponseImpl response = new ResponseImpl();
        String appName = request.getApp();
        if (DeployerConstants.MANAGE_TYPE_INSTANCE.equals(request.getManageType())) {
            appName = DeployerConstants.INSTANCE_APP_NAME;
        }
        App app = deployer.getApp(appName);
        app.invoke(request, response);

        // 将 request 收集的 session 参数，通过 response 回传到调用端
        response.getParametersInSession().putAll(request.getParametersInSession());

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
                            ? cryptoService.generateKey()
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
