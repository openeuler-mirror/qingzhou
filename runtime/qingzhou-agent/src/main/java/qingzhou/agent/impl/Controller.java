package qingzhou.agent.impl;

import qingzhou.engine.Module;

public class Controller implements Module {
    private ProcessSequence sequence;

    private Map<String, String> remoteConfig;
    private String remoteHost;
    private int remotePort;
    private ModuleContext moduleContext;
    private Config config;

    @Override
    public void start(ModuleContext moduleContext) throws Exception {
        this.moduleContext = moduleContext;

        config = moduleContext.getService(Config.class);
        remoteConfig = config.getDataById("remote", null);
        if (!Boolean.parseBoolean(remoteConfig.get("enabled"))) return;

        sequence = new ProcessSequence(
                new StartServer()
        );
        sequence.exec();
    }

    @Override
    public void stop() {
        if (sequence != null) {
            sequence.undo();
        }
    }

    private class StartServer implements Process {
        private HttpServer server;
        private String path;

        @Override
        public void exec() throws Exception {
            path = "/";
            remoteHost = remoteConfig.get("host");
            if (remoteHost == null) {
                remoteHost = "0.0.0.0";
            }
            remotePort = Integer.parseInt(remoteConfig.get("port"));
            server = moduleContext.getService(HttpServer.class);
            server.start(remoteHost, remotePort, 200);
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

            String serverUrl = "http://" + remoteHost + ":" + remotePort + path + context.getPath();
            moduleContext.getService(Logger.class).info("The remote service is started: " + serverUrl);
        }

        @Override
        public void undo() {
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
}
