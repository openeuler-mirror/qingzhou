package qingzhou.console.login;

import qingzhou.framework.console.ConsoleConstants;
import qingzhou.console.ConsoleUtil;
import qingzhou.console.ServerXml;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.controller.system.HttpServletContext;
import qingzhou.console.controller.system.I18nFilter;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.console.login.vercode.VerCode;
import qingzhou.console.sdk.ConsoleSDK;
import qingzhou.console.view.impl.HtmlView;
import qingzhou.console.view.impl.JsonView;
import qingzhou.framework.console.I18n;
import qingzhou.framework.console.Lang;
import qingzhou.framework.pattern.Filter;
import qingzhou.framework.util.IPUtil;
import qingzhou.framework.util.StringUtil;
import qingzhou.framework.util.TimeUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class LoginManager implements Filter<HttpServletContext> {
    public static final String LOGIN_USER = "j_username";
    public static final String LOGIN_PASSWORD = "j_password";

    public static final String LOGIN_ACCEPT_AGREEMENT = "acceptAgreement";
    public static final String LOGIN_PATH = "/login";
    public static final String LOGIN_URI = "/j_login";
    public static final String LOGOUT_FLAG = "invalidate";
    public static final String LOGIN_ERROR_MSG_KEY = "page.login.invalid";
    public static final String LOCKED_MSG_KEY = "page.login.locked";
    public static final String TWO_FA_MSG_KEY = "page.login.2fa";

    public static final String ACCEPT_AGREEMENT_MSG_KEY_MISSING = "page.login.agreement.missing";
    public static final String ACCEPT_AGREEMENT_MSG_KEY = "page.login.agreement";
    public static final String[] STATIC_RES_SUFFIX = {".html", ".js", ".css", ".ico", ".jpg", ".png", ".gif", ".ttf", ".woff", ".eot", ".svg", ".pdf"};
    private static final Map<String, LockOutRealm> userLockOutRealms = new LinkedHashMap<String, LockOutRealm>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, LockOutRealm> eldest) {
            return size() > 10000;
        }
    };

    public static LockOutRealm getLockOutRealm(HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        if (IPUtil.isLocalIp(clientIp)) { // 本机访问本机，浏览器、命令行、接口等可能用了不同的发送ip，有的是h127，有的是::1，有的实际ip等
            clientIp = "LocalIp";
        }
        return getLockOutRealm(clientIp);
    }

    public static LockOutRealm getLockOutRealm(String clientIp) {
        return userLockOutRealms.computeIfAbsent(clientIp, s -> new LockOutRealm());
    }

    public static String getUserPassword(String user) {
        Map<String, String> userP = ServerXml.get().user(user);
        if (userP == null) {
            return null;
        }
        return userP.get("password");
    }

    private static void webLogin(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // 对于浏览器的 web 登录，需要验证码校验
        LoginFailedMsg failedMsg = VerCode.checkVerCode(request);
        if (failedMsg == null) {
            failedMsg = login(request);
        }

        if (failedMsg == null) {
            try {
                // web 页面，设置默认的中文 i18n
                // 需要在登录成功后设置，这是为了保证存入到跳转前的 session 里面
                I18nFilter.setI18nLang(request, I18n.DEFAULT_LANG);

                // 进入主页
                response.sendRedirect(ConsoleUtil.encodeRedirectURL(request, response, request.getContextPath() + RESTController.INDEX_PATH)); // to welcome page
            } finally {
                I18n.resetI18nLang();
            }
        } else {
            String failedMsgKey = failedMsg.getHeaderMsg();
            String msg = LoginManager.retrieveI18nMsg(failedMsgKey);
            response.getWriter().print(JsonView.buildErrorResponse(msg));

            if (!response.isCommitted()) {
                String redirectUri = failedMsg.getRedirectUri();
                if (StringUtil.isBlank(redirectUri)) {
                    redirectUri = LOGIN_PATH + "?" + RESTController.MSG_FLAG + "=" + failedMsgKey;
                }

                String SP = redirectUri.contains("?") ? "&" : "?";
                if (!VerCode.isVerCodeDisabled()) {
                    redirectUri += (SP + VerCode.SHOW_CAPTCHA_FLAG); // 只要错误就要求输入验证码，命令行登录是不需要的，命令行设备5此登录失败拦截的，验证码只是前端拦截用户，本质没啥大用，因为命令行可以绕过验证码
                }
                response.sendRedirect(request.getContextPath() + redirectUri);
            }
        }
    }

    public static void setLoginUser(HttpSession session, String user) {
        if (user == null) {
            return;
        }
        user = encodeUser(user);
        session.setAttribute(LOGIN_USER, user);
    }

    public static String encodeUser(String user) {
        return Base64.getEncoder().encodeToString(user.getBytes(StandardCharsets.UTF_8));
    }

    public static String getLoginUser(HttpSession session) {
        if (session == null) {
            return null;
        }
        String user;
        try {
            user = (String) session.getAttribute(LOGIN_USER);
        } catch (Exception e) { // 可能是登陆页面，在首行 注销了
            return null;
        }
        if (user == null) {
            return null;
        }
        return new String(Base64.getDecoder().decode(user), StandardCharsets.UTF_8);
    }

    public static LoginFailedMsg login(HttpServletRequest request) throws Exception {
        String user = request.getParameter(LOGIN_USER);
        String password = request.getParameter(LOGIN_PASSWORD);
        if (StringUtil.isBlank(user) || StringUtil.isBlank(password)) {
            return new LoginFailedMsg("jmx.credentials.element.isNull", null);
        }
        String parameter = request.getParameter(LOGIN_ACCEPT_AGREEMENT);
        if (StringUtil.isBlank(parameter)) {
            return new LoginFailedMsg(ACCEPT_AGREEMENT_MSG_KEY_MISSING, null);
        } else {
            if (!Boolean.parseBoolean(parameter)) {
                return new LoginFailedMsg(ACCEPT_AGREEMENT_MSG_KEY, null);
            }
        }

        // 锁定了，就提前返回，不再验证密码
        LockOutRealm lockOutRealm = getLockOutRealm(request);
        if (lockOutRealm.isLocked(user)) {
            long left = lockOutRealm.getLockOutTime() - (TimeUtil.getCurrentTime() - lockOutRealm.getLockRecord(user).getLastFailureTime()) / 1000 + 1;
            if (left < 60) {
                left = 60;
            }
            String msgKey = LOCKED_MSG_KEY + "," + lockOutRealm.getFailureCount() + "," + left / 60;
            // login.jsp 已经在 application.xml 中配置了过滤，
            // 因此，不需要加：encodeRedirectURL，否则会在登录后的浏览器上显示出 csrf 的令牌值，反而有安全风险
            return new LoginFailedMsg(msgKey, null);
        }

        String checkPasswordError = checkPassword(user, password);
        boolean loginOk = checkPasswordError == null;
        loginOk = lockOutRealm.filterLockedAccounts(user, loginOk);
        if (loginOk) {
            // 密码通过以后，验证动态密码
            if (!check2FA(request)) {
                return new LoginFailedMsg(LoginManager.TWO_FA_MSG_KEY, null);
            }

            HttpSession session = request.getSession(false);
            if (session != null) {
                logout(request);// logout old user
            }
            session = request.getSession(true);

            // 登录成功，注册信息
            setLoginUser(session, user);

            // 注销在其它机器上忘记注销的会话: 命令行类似小型浏览器，登录一样要踢走浏览器用户，这是安全规则
            // ConsoleUtil.invalidateAllSessionAsAttribute(request, LOGIN_USER, (String) request.getSession().getAttribute(LOGIN_USER));todo

            return null;
        } else {
            String msgKey = LOGIN_ERROR_MSG_KEY;
            int failureCount;
            LockOutRealm.LockRecord lockRecord = lockOutRealm.getLockRecord(user);
            if (lockRecord != null) {
                failureCount = lockRecord.getFailures();
                msgKey = LOGIN_ERROR_MSG_KEY + "," + failureCount + "," + lockOutRealm.getFailureCount();
            }
            return new LoginFailedMsg(msgKey, null);
        }
    }

    private static boolean check2FA(HttpServletRequest request) throws Exception {
        return check2FA(request.getParameter(LOGIN_USER), ConsoleWarHelper.decryptWithConsolePrivateKey(request.getParameter(ConsoleConstants.LOGIN_2FA)));
    }

    public static boolean check2FA(String user, String login2FA) throws Exception {
        Map<String, String> userP = ServerXml.get().user(user);

        if (userP == null) return false;

        if (!Boolean.parseBoolean(userP.get("enable2FA"))) {
            return true; // 用户未开启双因子认证
        }

        if (!Boolean.parseBoolean(userP.get("bound2FA"))) {
            return true; // 放过，用户要去扫描二维码绑定密钥
        }

        if (StringUtil.isBlank(login2FA)) {
            return false;
        }

        String keyFor2FA = userP.get("keyFor2FA");
        return Totp.verify(keyFor2FA, login2FA);
    }

    public static String checkPassword(String user, String password) {
        try {
            password = ConsoleWarHelper.decryptWithConsolePrivateKey(password);
        } catch (Exception ignored) {
        }

        try {
            String userPwd = getUserPassword(user);
            if (ConsoleWarHelper.getCryptoService().getMessageDigest().matches(password, userPwd)) {
                return null;
            } else {
                return LOGIN_ERROR_MSG_KEY;
            }
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null) {
                msg = "";
            } else {
                msg = ": " + msg;
            }
            return "An error occurred in the password verification process" + msg;
        }
    }

    public static void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String user = getLoginUser(session);
            if (StringUtil.notBlank(user)) {
                session.invalidate();
            }
        }
    }

    public static String retrieveI18nMsg(String webLoginMsg) { // 来自 head.jsp
        String common_msg = webLoginMsg;
        if (StringUtil.notBlank(common_msg)) {
            String[] split = common_msg.split(",");
            if (split.length < 2) {
                common_msg = getMsg(common_msg);
            } else {
                String[] args = Arrays.copyOfRange(split, 1, split.length);
                common_msg = String.format(getMsg(split[0]), (Object[]) args);
            }
        }
        return common_msg;
    }

    private static String getMsg(String msg) {
        String i18n = I18n.getString(ConsoleConstants.MASTER_APP_NAME, msg);
        return StringUtil.notBlank(i18n) ? i18n : msg;
    }

    @Override
    public boolean doFilter(HttpServletContext context) throws Exception {
        HttpServletRequest request = context.req;
        HttpServletResponse response = context.resp;
        String checkPath = ConsoleUtil.retrieveServletPathAndPathInfo(request);

        if (checkPath.startsWith("/static/")) {
            for (String suffix : STATIC_RES_SUFFIX) {
                if (checkPath.endsWith(suffix)) {
                    return true;
                }
            }
        }

        if (checkPath.equals(LOGIN_PATH)) {
            if (request.getParameter(LOGOUT_FLAG) != null) {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    LoginManager.logout(request);
                }
            }
            request.getRequestDispatcher(HtmlView.htmlPageBase + "login.jsp").forward(request, response);
            return false;
        }

        if (checkPath.equals(LOGIN_URI)) {
            try {
                I18nFilter.setI18nLang(request, I18n.DEFAULT_LANG); // 确保登录页面（包括出错信息）都以中文信息展示
                webLogin(request, response);
            } catch (Exception e) {
                if (e instanceof IOException) {
                    throw e;
                } else {
                    throw new IllegalStateException(e);
                }
            } finally {
                I18n.resetI18nLang();
            }
            return false;
        }

        // 拦截未登录的请求
        HttpSession session = request.getSession(false);
        String user = null;
        if (session != null) {
            user = getLoginUser(session);
        }
        if (session == null || user == null) {
            if (!response.isCommitted()) {
                if (request.getHeader("accept") != null && request.getHeader("accept").contains("application/json")) {
                    response.setContentType("application/json;charset=UTF-8");
                    try (PrintWriter writer = context.resp.getWriter()) {
                        writer.write("{\"success\":\"false\",\"msg\":\"" + I18n.getString(ConsoleConstants.MASTER_APP_NAME, "page.login.need") + "\"}");
                        writer.flush();
                    }
                    return false;
                }
                // login.jsp 已经在 application.xml 中配置了过滤，
                // 因此，不需要加：encodeRedirectURL，否则会在登录后的浏览器上显示出 csrf 的令牌值，反而有安全风险
                String toJson = JsonView.buildErrorResponse("Please enter username and password to log in to the system");
                response.getWriter().print(toJson);
                if (I18n.getI18nLang() == Lang.en) { // header里只能英文
                    response.setHeader(ConsoleConstants.RESPONSE_HEADER_MSG_KEY, toJson);
                } else {
                    response.setHeader(ConsoleConstants.RESPONSE_HEADER_MSG_KEY, ConsoleSDK.encodeId(toJson));
                }
                response.sendRedirect(request.getContextPath() + LOGIN_PATH);
            }
            return false;
        }

        // here, Everything looks good
        return true;
    }

    public static class LoginFailedMsg {
        private final String headerMsg;
        private final String redirectUri;

        public LoginFailedMsg(String headerMsg, String redirectUri) {
            this.headerMsg = headerMsg;
            this.redirectUri = redirectUri;
        }

        public String getHeaderMsg() {
            return headerMsg;
        }

        public String getRedirectUri() {
            return redirectUri;
        }
    }
}
