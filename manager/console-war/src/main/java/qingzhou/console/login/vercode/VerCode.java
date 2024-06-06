package qingzhou.console.login.vercode;

import qingzhou.console.controller.HttpServletContext;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.rest.AsymmetricDecryptor;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.i18n.ConsoleI18n;
import qingzhou.console.login.LockOutRealm;
import qingzhou.console.login.LoginManager;
import qingzhou.console.util.IPUtil;
import qingzhou.engine.util.pattern.Filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class VerCode implements Filter<HttpServletContext> {
    public static final int verCodeCount = 1;

    public static final String CAPTCHA_URI = "/captcha";

    public static final String CAPTCHA = "j_captcha";

    public static final String SHOW_CAPTCHA_FLAG = "SHOW_CAPTCHA_FLAG";
    public static final String captchaError = "page.login.captchaError";

    private static final Map<String, Map<String, String>> userVerCodes = new LinkedHashMap<String, Map<String, String>>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Map<String, String>> eldest) {
            return size() > 10000;
        }
    };

    static {
        ConsoleI18n.addI18n(captchaError, new String[]{"登录失败，验证码错误", "en:Login failed, verification code error"});
    }

    private final String verCodeFormat = "jpeg";
    private final Captcha captcha = new Captcha(verCodeFormat);
    private final char[] CHAR_ARRAY = "3456789ABCDEFGHJKMNPQRSTUVWXY".toCharArray();

    public static Map<String, String> getUserVerCodes(HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        if (IPUtil.isLocalIp(clientIp)) { // 本机访问本机，浏览器、命令行、接口等可能用了不同的发送ip，有的是h127，有的是::1，有的实际ip等
            clientIp = "LocalIp";
        }
        return userVerCodes.computeIfAbsent(clientIp, s -> new HashMap<>());
    }

    public static boolean isVerCodeDisabled() {
        return !SystemController.getConsole().getSecurity().isVerCodeEnabled();
    }

    public static LoginManager.LoginFailedMsg checkVerCode(HttpServletRequest request) {
        if (isVerCodeDisabled()) {
            return null;
        }

        String user = request.getParameter(LoginManager.LOGIN_USER);
        LockOutRealm.LockRecord lockRecord = LoginManager.getLockOutRealm(request).getLockRecord(user);
        if (lockRecord != null) {
            int failureCount = lockRecord.getFailures();
            if (failureCount >= verCodeCount) {
                if (!validate(request)) {
                    // login.jsp 已经在 application.xml 中配置了过滤，
                    // 因此，不需要加：encodeRedirectURL，否则会在登录后的浏览器上显示出 csrf 的令牌值，反而有安全风险
                    return new LoginManager.LoginFailedMsg(captchaError,
                            LoginManager.LOGIN_PATH + "?" + RESTController.MSG_FLAG + "=" + captchaError);
                }
            }
        }
        return null;
    }

    /**
     * 校验用户输入的验证码是否正确
     */
    public static boolean validate(HttpServletRequest request) {
        String clientCode = AsymmetricDecryptor.decryptWithConsolePrivateKey(request.getParameter(CAPTCHA));
        if (clientCode == null) {
            return false;
        }

        Map<String, String> userVerCodes = getUserVerCodes(request);
        if (userVerCodes == null) {
            return false;
        }
        String serverCode = userVerCodes.remove(CAPTCHA);// remove: 及时销毁使用过的验证码

        return clientCode.equalsIgnoreCase(serverCode);
    }

    @Override
    public boolean doFilter(HttpServletContext context) throws Exception {
        if (isVerCodeDisabled()) {
            return true;
        }

        HttpServletRequest request = context.req;
        HttpServletResponse response = context.resp;
        String checkPath = RESTController.retrieveServletPathAndPathInfo(request);

        if (checkPath.equals(CAPTCHA_URI)) {
            render(request, response);
            return false;
        }

        return true;
    }

    /**
     * 生成验证码
     */
    private void render(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String verCode = generateRandomString();
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/" + verCodeFormat);
        getUserVerCodes(request).put(CAPTCHA, verCode);// getSession(true): for #ITAIT-3763
        captcha.render(verCode, response.getOutputStream());
    }

    private String generateRandomString() {
        Random random = new Random(System.nanoTime());
        char[] randomChars = new char[4];
        for (int i = 0; i < randomChars.length; i++) {
            randomChars[i] = CHAR_ARRAY[random.nextInt(CHAR_ARRAY.length)];
        }
        return String.valueOf(randomChars);
    }
}
