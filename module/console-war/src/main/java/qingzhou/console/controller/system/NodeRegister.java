package qingzhou.console.controller.system;

import qingzhou.console.ConsoleConstants;
import qingzhou.console.ConsoleUtil;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.PublicKeyCipher;
import qingzhou.framework.pattern.Filter;

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
        String checkPath = ConsoleUtil.retrieveServletPathAndPathInfo(request);
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
        if (arg != null) {
            CryptoService cryptoService = ConsoleWarHelper.getCryptoService();
            String privateKey = cryptoService.getKeyManager().getKeyPair(null, ConsoleConstants.privateKeyName, ConsoleConstants.privateKeyName, ConsoleConstants.privateKeyName);
            PublicKeyCipher publicKeyCipher = cryptoService.getPublicKeyCipher(null, privateKey);
            arg = publicKeyCipher.decryptWithPrivateKey(arg);
            Map<String, String> params = parseArg(arg, "utf-8");
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
                    // todo 生成 appStub 代理
                    ConsoleWarHelper.registerApp(appToken, null);
                }
            }
        }
        System.out.printf("Node Registration Done. ip:%s, port:%s.%n", nodeIp, nodePort);
        return false;
    }

    private Map<String, String> parseArg(String arg, String encoding) throws UnsupportedEncodingException {
        Map<String, String> map = new HashMap<>();
        if (arg == null) {
            return map;
        }
        String[] split = arg.split("&");
        for (String s : split) {
            int i = s.indexOf("=");
            String key = s.substring(0, i);
            String value = URLDecoder.decode(s.substring(i + 1), encoding);
            map.put(key, value);
        }
        return map;
    }

    private String buildAppToken(String ip, String port, String app) {
        return String.format("%s:%s/%s", ip, port, app);
    }
}
