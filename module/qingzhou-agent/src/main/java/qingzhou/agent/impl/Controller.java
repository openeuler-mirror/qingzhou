package qingzhou.agent.impl;

import qingzhou.config.Agent;
import qingzhou.config.Config;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.PairCipher;
import qingzhou.deployer.*;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.Service;
import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Process;
import qingzhou.engine.util.pattern.ProcessSequence;
import qingzhou.http.Http;
import qingzhou.http.HttpContext;
import qingzhou.http.HttpResponse;
import qingzhou.http.HttpServer;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.AppInfo;
import qingzhou.registry.InstanceInfo;
import qingzhou.registry.ModelInfo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

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

    private ProcessSequence sequence;
    private String agentHost;
    private int agentPort;

    @Override
    public void start(ModuleContext moduleContext) throws Exception {
        sequence = new ProcessSequence(
                new ResponseService(),
                new Heartbeat()
        );
        sequence.exec();
    }

    @Override
    public void stop() {
        sequence.undo();
    }

    private class ResponseService implements Process {
        private String path;
        private HttpServer server;
        private String agentKey;

        @Override
        public void exec() throws Exception {
            Agent agent = config.getAgent();
            if (agent == null || !agent.isEnabled()) return;

            path = "/";
            server = http.buildHttpServer();
            agentHost = agent.getAgentHost();
            if (agentHost == null || agentHost.isEmpty()) {
                agentHost = "0.0.0.0";
            }
            agentPort = agent.getAgentPort();
            server.start(agentHost, agentPort, 200);
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
                    if (result == null || result.length == 0) return;

                    try (OutputStream outputStream = exchange.getResponseBody()) {
                        outputStream.write(result);
                    }
                } finally {
                    exchange.close();
                }
            });

            String serverUrl = "http://" + agentHost + ":" + agentPort + context.getPath();
            logger.info("The agent service is started: " + serverUrl);
        }

        @Override
        public void undo() {
            if (server == null) return;
            server.removeContext(path);
            server.stop(0);
        }

        byte[] process(InputStream in) throws Exception {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(in.available());
            FileUtil.copyStream(in, bos);

            // 1. 获得请求的数据
            byte[] requestData = bos.toByteArray();
            if (requestData.length == 0) return null;

            // 2. 数据解密，带认证
            if (agentKey == null) {
                Agent agent = config.getAgent();
                agentKey = agent.getAgentKey() == null || agent.getAgentKey().isEmpty()
                        ? cryptoService.generateKey()
                        : agent.getAgentKey();
            }

            Cipher cipher = cryptoService.getCipher(agentKey);
            byte[] decryptedData = cipher.decrypt(requestData);

            // 3. 得到请求对象
            RequestImpl request = json.fromJson(new String(decryptedData, DeployerConstants.ACTION_INVOKE_CHARSET), RequestImpl.class);

            // 4. 处理
            App app = deployer.getApp(request.getApp());
            preProcess(request, app);
            app.invoke(request);

            // 将 request 收集的 session 参数，通过 response 回传到调用端
            ResponseImpl response = (ResponseImpl) request.getResponse();
            response.getParametersInSession().putAll(request.getParametersInSession());

            // 5. 响应数据
            byte[] responseData = json.toJson(response).getBytes(DeployerConstants.ACTION_INVOKE_CHARSET);

            // 6. 数据加密，返回到客户端
            return cipher.encrypt(responseData);
        }

        private void preProcess(RequestImpl request, App app) {
            ModelInfo modelInfo = request.getCachedModelInfo();
            String[] uploadFieldNames = modelInfo.getFileUploadFieldNames();
            if (uploadFieldNames == null) return;
            for (String uploadField : uploadFieldNames) {
                String uploadFile = request.getParameter(uploadField);
                if (!uploadFile.startsWith(DeployerConstants.UPLOAD_FILE_PREFIX_FLAG)) continue;

                String uploadId = uploadFile.substring(DeployerConstants.UPLOAD_FILE_PREFIX_FLAG.length());
                File uploadDir = FileUtil.newFile(app.getAppContext().getTemp(), DeployerConstants.UPLOAD_FILE_TEMP_SUB_DIR, uploadId);
                if (!uploadDir.isDirectory()) continue;

                File[] listFiles = uploadDir.listFiles();
                if (listFiles == null || listFiles.length != 1) continue;

                request.setParameter(uploadField, listFiles[0].getAbsolutePath());
            }
        }
    }

    private class Heartbeat implements Process {
        // 定时器设计目的：解决 master 未启动或者宕机重启等引起的注册失效问题
        private Timer timer;
        private InstanceInfo thisInstanceInfo;

        @Override
        public void exec() {
            Agent agent = config.getAgent();
            if (agent == null || !agent.isEnabled()) return;

            thisInstanceInfo = thisInstanceInfo();
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        register();
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }, 2000, 1000 * 30);
        }

        @Override
        public void undo() {
            if (timer != null) {
                timer.cancel();
            }
        }

        void register() throws Exception {
            String masterUrl = config.getAgent().getMasterUrl();
            if (masterUrl == null || masterUrl.trim().isEmpty()) {
                logger.warn("Instance registration fails: \"masterUrl\" is not set correctly.");
                return;
            } else if (masterUrl.endsWith("/")) {
                masterUrl = masterUrl.substring(0, masterUrl.length() - 1);
            }

            List<AppInfo> appInfos = new ArrayList<>();
            for (String a : deployer.getAllApp()) {
                if (DeployerConstants.APP_SYSTEM.equals(a)) continue;
                appInfos.add(deployer.getApp(a).getAppInfo());
            }
            thisInstanceInfo.setAppInfos(appInfos.toArray(new AppInfo[0]));

            String registerData = json.toJson(thisInstanceInfo);

            boolean registered = false;
            String baseUri = masterUrl + DeployerConstants.REST_PREFIX + "/" + DeployerConstants.JSON_VIEW + "/" + DeployerConstants.APP_SYSTEM + "/" + DeployerConstants.MODEL_MASTER + "/";
            try {
                String fingerprint = cryptoService.getMessageDigest().fingerprint(registerData);
                HttpResponse response = http.buildHttpClient().send(baseUri + DeployerConstants.ACTION_CHECK, new HashMap<String, String>() {{
                    put(DeployerConstants.CHECK_FINGERPRINT, fingerprint);
                }});
                if (response.getResponseCode() == 200) {
                    Map resultMap = json.fromJson(new String(response.getResponseBody(), DeployerConstants.ACTION_INVOKE_CHARSET), Map.class);
                    List<Map<String, String>> dataList = (List<Map<String, String>>) resultMap.get(DeployerConstants.JSON_DATA);
                    if (dataList != null && !dataList.isEmpty()) {
                        String checkResult = dataList.get(0).get(fingerprint);
                        registered = Boolean.parseBoolean(checkResult);
                    }
                }
            } catch (Throwable e) {
                logger.warn("An exception occurred during the registration process", e);
            }
            if (registered) return;

            http.buildHttpClient().send(baseUri + DeployerConstants.ACTION_REGISTER, new HashMap<String, String>() {{
                put("doRegister", registerData);
            }});
        }

        private InstanceInfo thisInstanceInfo() {
            InstanceInfo instanceInfo = new InstanceInfo();
            instanceInfo.setName(UUID.randomUUID().toString().replace("-", ""));
            Agent agent = config.getAgent();
            instanceInfo.setHost(agentHost.equals("0.0.0.0")
                    ? Utils.getLocalIps().iterator().next()
                    : agentHost);
            instanceInfo.setPort(agentPort);

            PairCipher pairCipher;
            try {
                pairCipher = cryptoService.getPairCipher(agent.getMasterKey(), null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            String key = pairCipher.encryptWithPublicKey(agent.getAgentKey());
            instanceInfo.setKey(key);
            return instanceInfo;
        }
    }
}
