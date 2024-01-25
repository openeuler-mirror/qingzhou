package qingzhou.console.controller.system;

import qingzhou.console.ConsoleConstants;
import qingzhou.console.ConsoleUtil;
import qingzhou.console.ServerXml;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.console.page.PageBackendService;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.PasswordCipher;
import qingzhou.crypto.PublicKeyCipher;
import qingzhou.framework.AppStub;
import qingzhou.framework.pattern.Filter;
import qingzhou.framework.util.XmlUtil;
import qingzhou.remote.impl.net.Channel;
import qingzhou.remote.impl.net.bio.BIOConnector;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
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
        CryptoService cryptoService = ConsoleWarHelper.getCryptoService();
        if (arg != null) {
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
                    AppStub appStub = (AppStub) Proxy.newProxyInstance(AppStub.class.getClassLoader(), new Class[]{AppStub.class}, (proxy, method, args) -> {
                        InetSocketAddress socketAddress = new InetSocketAddress(nodeIp, Integer.parseInt(nodePort));
                        BIOConnector connector = new BIOConnector(socketAddress);
                        connector.setCodec(null);
                        connector.setHandler(null);
                        Channel channel = connector.connect();
                        // todo 封装对象 req
                        Object req = null;
                        channel.write(req);
                        return channel.read();
                    });
                    ConsoleWarHelper.registerApp(appToken, appStub);
                }
            }
        }
        PasswordCipher passwordCipher = getPasswordCipher(cryptoService);
        Map<String, String> node = new HashMap<>();
        node.put("id", nodeIp + ":" + nodePort);
        node.put("nodeIp", nodeIp);
        node.put("nodePort", nodePort);
        node.put("apps", apps);
        node.put("key", passwordCipher.encrypt(key)); // todo 是否持久化，考虑每次重新注册后生成新的key
        XmlUtil xmlUtil = new XmlUtil(ServerXml.getServerXml());
        xmlUtil.setAttributes("/server/nodes/node", node);
        xmlUtil.write();

        System.out.printf("Node Registration Done. ip:%s, port:%s.%n", nodeIp, nodePort);
        return false;
    }

    private static PasswordCipher getPasswordCipher(CryptoService cryptoService) throws Exception {
        String localKey = cryptoService.getKeyManager().getKey(PageBackendService.getSecureFile(ConsoleWarHelper.getDomain()), ConsoleConstants.localKeyName);
        return cryptoService.getPasswordCipher(localKey);
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
