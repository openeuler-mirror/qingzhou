package qingzhou.core.agent.impl;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import qingzhou.core.DeployerConstants;
import qingzhou.core.deployer.AppManager;
import qingzhou.core.deployer.Deployer;
import qingzhou.core.deployer.RequestImpl;
import qingzhou.core.deployer.ResponseImpl;
import qingzhou.crypto.Cipher;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Process;
import qingzhou.http.Http;
import qingzhou.http.HttpContext;
import qingzhou.http.HttpServer;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.serializer.Serializer;

class Service implements Process {
    private final ModuleContext moduleContext;
    private final String agentHost;
    private final int agentPort;
    private String path;
    private HttpServer server;
    private final Cipher agentCipher;

    Service(ModuleContext moduleContext, Cipher agentCipher, String agentHost, int agentPort) {
        this.moduleContext = moduleContext;
        this.agentCipher = agentCipher;
        this.agentHost = agentHost;
        this.agentPort = agentPort;
    }

    @Override
    public void exec() throws Exception {
        Logger logger = moduleContext.getService(Logger.class);

        path = "/";
        server = moduleContext.getService(Http.class).buildHttpServer();

        server.start(agentHost, agentPort, 200);
        HttpContext context = server.createContext(path);
        context.setHandler(exchange -> {
            try {
                byte[] result;
                try (InputStream inputStream = exchange.getRequestBody()) {
                    result = process(inputStream);
                    exchange.setStatus(200);
                } catch (Throwable e) {
                    Throwable cause = Utils.getCause(e);
                    String error = Utils.exceptionToString(cause);
                    logger.error(error);
                    result = error.getBytes(StandardCharsets.UTF_8);
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

        logger.info(String.format("The agent service is started, host: %s, port: %s", agentHost, agentPort));
    }

    @Override
    public void undo() {
        if (server != null) {
            server.removeContext(path);
            server.stop(0);
        }
    }

    private byte[] process(InputStream in) throws Throwable {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(in.available());
        FileUtil.copyStream(in, bos);

        // 1. 获得请求的数据
        byte[] requestData = bos.toByteArray();
        if (requestData.length == 0) return null;

        // 2. 数据解密，带认证
        byte[] decryptedData = agentCipher.decrypt(requestData);

        // 3. 得到请求对象
        RequestImpl request = moduleContext.getService(Json.class).fromJson(new String(decryptedData, DeployerConstants.ACTION_INVOKE_CHARSET), RequestImpl.class);

        // 4. 处理
        AppManager appManager = moduleContext.getService(Deployer.class).getApp(request.getApp());
        List<File> uploadDirs = uploadDirs(request, appManager);
        try {
            appManager.invoke(request);
        } finally {
            uploadDirs.forEach(FileUtil::forceDeleteQuietly);
        }

        // 将 request 收集的 session 参数，通过 response 回传到调用端
        ResponseImpl response = (ResponseImpl) request.getResponse();
        Serializable appData = response.getAppData();
        if (appData != null) { // 数据清洗提炼，减少传输压力
            ResponseImpl liteResponse = new ResponseImpl();
            liteResponse.setData(appData);
            response = liteResponse;
        }
        response.getParametersInSession().putAll(request.parametersForSession());

        // 5. 响应数据
        return moduleContext.getService(Serializer.class).serialize(response);
    }

    private List<File> uploadDirs(RequestImpl request, AppManager appManager) {
        List<File> uploadDirs = new ArrayList<>();
        request.setCachedModelInfo(appManager.getAppInfo().getModelInfo(request.getModel()));
        Set<String> parameterNames = request.getParameters().keySet();
        for (String uploadField : parameterNames) {
            String detectUploadFile = request.getParameter(uploadField);
            if (Utils.isBlank(detectUploadFile) ||
                    !detectUploadFile.startsWith(DeployerConstants.UPLOAD_FILE_PREFIX_FLAG)) continue;

            String uploadId = detectUploadFile.substring(DeployerConstants.UPLOAD_FILE_PREFIX_FLAG.length());
            File uploadDir = FileUtil.newFile(appManager.getAppContext().getTemp(), DeployerConstants.UPLOAD_FILE_TEMP_SUB_DIR, uploadId);
            if (!uploadDir.isDirectory()) continue;
            uploadDirs.add(uploadDir);

            File[] listFiles = uploadDir.listFiles();
            if (listFiles == null || listFiles.length != 1) continue;

            request.getParameters().put(uploadField, listFiles[0].getAbsolutePath());
        }
        return uploadDirs;
    }
}
