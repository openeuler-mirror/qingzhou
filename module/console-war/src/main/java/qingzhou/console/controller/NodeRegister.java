package qingzhou.console.controller;

import qingzhou.console.AppMetadataManager;
import qingzhou.console.ConsoleConstants;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.framework.config.Config;
import qingzhou.framework.crypto.CryptoService;
import qingzhou.framework.crypto.KeyCipher;
import qingzhou.framework.crypto.KeyPairCipher;
import qingzhou.framework.util.pattern.Filter;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class NodeRegister implements Filter<HttpServletContext> {

    @Override
    public boolean doFilter(HttpServletContext context) throws Exception {
        HttpServletRequest request = context.req;
        String checkPath = RESTController.retrieveServletPathAndPathInfo(request);
        if (!checkPath.equals(ConsoleConstants.REGISTER_URI)) {
            return true;
        }

        Map<String, String> map = new HashMap<>();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            String value = request.getParameter(name);
            map.put(name, value);
        }
        String arg = map.get("A");
        CryptoService cryptoService = SystemController.getCryptoService();
        if (arg != null) {
            String privateKey = SystemController.getConfig().getKey(Config.privateKeyName);
            KeyPairCipher keyPairCipher = cryptoService.getKeyPairCipher(null, privateKey);
            arg = keyPairCipher.decryptWithPrivateKey(arg);
            Map<String, String> params = parseArg(arg);
            map.putAll(params);
        }
        String nodeIp = map.get("nodeIp");
        String nodePort = map.get("nodePort");
        String apps = map.get("apps");
        String key = map.get("key");
        if (apps != null) {
            for (String app : apps.split(",")) {
                if (app != null && !app.trim().isEmpty()) {
                    String appToken = buildAppToken(nodeIp, nodePort, app);
// TODO 这块需要改成 Service 方式，只依赖接口，不要依赖实现，另外 console 模块不能依赖 remote 模块，否则就形成，因为设计上 remote 是依赖 console 的。
//                    AppStub appStub = (AppStub) Proxy.newProxyInstance(AppStub.class.getClassLoader(), new Class[]{AppStub.class}, (proxy, method, args) -> {
//                        InetSocketAddress socketAddress = new InetSocketAddress(nodeIp, Integer.parseInt(nodePort));
//                        BIOConnector connector = new BIOConnector(socketAddress);
//                        connector.setCodec(null);
//                        connector.setHandler(null);
//                        Channel channel = connector.connect();
//                        // todo 封装对象 req
//                        Object req = null;
//                        channel.write(req);
//                        return channel.read();
//                    });
                    AppMetadataManager.getInstance().registerAppStub(appToken, null);// todo 序列化过来吗？
                }
            }
        }
        String localKey = SystemController.getConfig().getKey(Config.localKeyName);
        KeyCipher keyCipher = cryptoService.getKeyCipher(localKey);
        Map<String, String> node = new HashMap<>();
        node.put("id", nodeIp + ":" + nodePort);
        node.put("nodeIp", nodeIp);
        node.put("nodePort", nodePort);
        node.put("apps", apps);
        node.put("key", keyCipher.encrypt(key)); // todo 是否持久化，考虑每次重新注册后生成新的key
        SystemController.getConfig().updateConfig("/server/nodes/node", node);

        System.out.printf("Node Registration Done. ip:%s, port:%s.%n", nodeIp, nodePort);
        return false;
    }

    private Map<String, String> parseArg(String arg) throws UnsupportedEncodingException {
        Map<String, String> map = new HashMap<>();
        if (arg == null) {
            return map;
        }
        String[] split = arg.split("&");
        for (String s : split) {
            int i = s.indexOf("=");
            String key = s.substring(0, i);
            String value = URLDecoder.decode(s.substring(i + 1), "utf-8");
            map.put(key, value);
        }
        return map;
    }

    private String buildAppToken(String ip, String port, String app) {
        return String.format("%s:%s/%s", ip, port, app);
    }
}
