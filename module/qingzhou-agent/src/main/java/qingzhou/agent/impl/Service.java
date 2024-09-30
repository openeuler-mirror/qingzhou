package qingzhou.agent.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import qingzhou.crypto.Cipher;
import qingzhou.deployer.App;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.RequestImpl;
import qingzhou.deployer.ResponseImpl;
import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Process;
import qingzhou.http.Http;
import qingzhou.http.HttpContext;
import qingzhou.http.HttpServer;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.ModelInfo;

class Service implements Process {
    private final Http http;
    private final Logger logger;
    private final Json json;
    private final Deployer deployer;

    private final Cipher agentCipher;
    private final String agentHost;
    private final int agentPort;
    private String path;
    private HttpServer server;

    Service(String agentHost, int agentPort, Cipher agentCipher, Http http, Logger logger, Json json, Deployer deployer) {
        this.agentHost = agentHost;
        this.agentPort = agentPort;
        this.agentCipher = agentCipher;
        this.http = http;
        this.logger = logger;
        this.json = json;
        this.deployer = deployer;
    }

    @Override
    public void exec() throws Exception {
        path = "/";
        server = http.buildHttpServer();

        server.start(agentHost, agentPort, 200);
        HttpContext context = server.createContext(path);
        context.setHandler(exchange -> {
            try {
                byte[] result;
                try (InputStream inputStream = exchange.getRequestBody()) {
                    result = process(inputStream);
                    exchange.setStatus(200);
                } catch (Exception e) {
                    result = Utils.exceptionToString(e).getBytes(StandardCharsets.UTF_8);
                    exchange.setStatus(500);
                }
                if (result == null || result.length == 0) return;

                // 加密数据，返回到客户端
                try {
                    result = agentCipher.encrypt(result);
                } catch (Exception e) {
                    result = Utils.exceptionToString(e).getBytes(StandardCharsets.UTF_8);
                }

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
        if (server != null) {
            server.removeContext(path);
            server.stop(0);
        }
    }

    private byte[] process(InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(in.available());
        FileUtil.copyStream(in, bos);

        // 1. 获得请求的数据
        byte[] requestData = bos.toByteArray();
        if (requestData.length == 0) return null;

        // 2. 数据解密，带认证
        byte[] decryptedData = agentCipher.decrypt(requestData);

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
        return json.toJson(response).getBytes(DeployerConstants.ACTION_INVOKE_CHARSET);
    }

    private void preProcess(RequestImpl request, App app) {
        request.setCachedModelInfo(app.getAppInfo().getModelInfo(request.getModel()));
        ModelInfo modelInfo = request.getCachedModelInfo();
        String[] uploadFieldNames = modelInfo.getFileUploadFieldNames();
        if (uploadFieldNames == null) return;
        for (String uploadField : uploadFieldNames) {
            String uploadFile = request.getParameter(uploadField);
            if (Utils.isBlank(uploadFile) ||
                    !uploadFile.startsWith(DeployerConstants.UPLOAD_FILE_PREFIX_FLAG)) continue;

            String uploadId = uploadFile.substring(DeployerConstants.UPLOAD_FILE_PREFIX_FLAG.length());
            File uploadDir = FileUtil.newFile(app.getAppContext().getTemp(), DeployerConstants.UPLOAD_FILE_TEMP_SUB_DIR, uploadId);
            if (!uploadDir.isDirectory()) continue;

            File[] listFiles = uploadDir.listFiles();
            if (listFiles == null || listFiles.length != 1) continue;

            request.setParameter(uploadField, listFiles[0].getAbsolutePath());
        }
    }
}
