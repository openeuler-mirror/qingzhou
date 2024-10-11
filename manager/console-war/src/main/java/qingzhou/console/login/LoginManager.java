package qingzhou.console.login;

import qingzhou.config.User;
import qingzhou.console.IPUtil;
import qingzhou.console.controller.I18n;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.SystemControllerContext;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.login.vercode.VerCode;
import qingzhou.console.view.type.HtmlView;
import qingzhou.console.view.type.JsonView;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.TotpCipher;
import qingzhou.deployer.DeployerConstants;
import qingzhou.engine.util.pattern.Filter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class LoginManager implements Filter<SystemControllerContext> {
    public static final String LOGIN_PATH = "/login";
    public static final String LOGIN_URI = "/j_login";
    public static final String LOGOUT_FLAG = "invalidate";

    public static final String LOGIN_USER = "j_username";
    public static final String LOGIN_PASSWORD = "j_password";
    public static final String LOGIN_OTP = "otp";

    public static final String RESPONSE_HEADER_MSG_KEY = "HEADER_MSG_KEY";
    public static final String INDEX_PATH = DeployerConstants.REST_PREFIX + "/" + HtmlView.FLAG + "/" + DeployerConstants.APP_SYSTEM + "/" + DeployerConstants.MODEL_INDEX + "/" + DeployerConstants.ACTION_INDEX;

    private static final String LOGIN_ERROR_MSG_KEY = "page.login.invalid";
    private static final String LOCKED_MSG_KEY = "page.login.locked";
    private static final String LOGIN_CAPTCHA_ERROR = "page.login.captchaError";

    private static final String[] STATIC_RES_SUFFIX = {".html", ".js", ".css", ".ico", ".jpg", ".png", ".gif", ".ttf", ".woff", ".eot", ".svg", ".pdf"};

    static {
        I18n.addKeyI18n(LOGIN_CAPTCHA_ERROR, new String[]{"登录失败，验证码错误", "en:Login failed, verification code error"});
        I18n.addKeyI18n(LOGIN_ERROR_MSG_KEY, new String[]{"登录失败，用户名或密码错误。当前登录失败 %s 次，连续失败 %s 次，账户将锁定", "en:Login failed, wrong username or password. The current login failed %s times, and the account will be locked after %s consecutive failures"});
        I18n.addKeyI18n(LOCKED_MSG_KEY, new String[]{"连续登录失败 %s 次，账户已经锁定，请 %s 分钟后重试", "en:Login failed %s times in a row, account is locked, please try again in %s minutes"});
    }

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

    private static void webLogin(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // 对于浏览器的 web 登录，需要验证码校验
        LoginFailedMsg failedMsg = checkVerCode(request);
        if (failedMsg == null) {
            failedMsg = login(request);
        }

        if (failedMsg == null) {
            try {
                // web 页面，设置默认的中文 i18n
                // 需要在登录成功后设置，这是为了保证存入到跳转前的 session 里面
                I18n.resetI18nLang();

                // 进入主页
                response.sendRedirect(RESTController.encodeURL(response, request.getContextPath() + INDEX_PATH)); // to welcome page
            } finally {
                I18n.resetI18nLang();
            }
        } else {
            String failedMsgKey = failedMsg.getHeaderMsg();
            String msg = LoginManager.retrieveI18nMsg(failedMsgKey);
            JsonView.responseErrorJson(response, msg);

            if (!response.isCommitted()) {
                String redirectUri = failedMsg.getRedirectUri();
                if (redirectUri == null) {
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

    private static LoginManager.LoginFailedMsg checkVerCode(HttpServletRequest request) {
        if (VerCode.isVerCodeDisabled()) return null;

        String user = request.getParameter(LoginManager.LOGIN_USER);
        LockOutRealm.LockRecord lockRecord = LoginManager.getLockOutRealm(request).getLockRecord(user);
        if (lockRecord != null) {
            int failureCount = lockRecord.getFailures();
            int verCodeCount = 1;
            if (failureCount >= verCodeCount) {
                if (!VerCode.validate(request)) {
                    // login.jsp 已经在 application.xml 中配置了过滤，
                    // 因此，不需要加：encodeRedirectURL，否则会在登录后的浏览器上显示出 csrf 的令牌值，反而有安全风险
                    return new LoginManager.LoginFailedMsg(LOGIN_CAPTCHA_ERROR,
                            LoginManager.LOGIN_PATH + "?" + RESTController.MSG_FLAG + "=" + LOGIN_CAPTCHA_ERROR);
                }
            }
        }
        return null;
    }

    private static void setLoginUser(HttpSession session, String user) {
        if (user == null) return;
        session.setAttribute(LOGIN_USER, user);
    }

    public static String getLoginUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;

        try {
            return (String) session.getAttribute(LOGIN_USER);
        } catch (Exception e) { // 可能是登陆页面，在首行 注销了
            return null;
        }
    }

    public static LoginFailedMsg login(HttpServletRequest request) throws Exception {
        String user = request.getParameter(LOGIN_USER);
        String password = request.getParameter(LOGIN_PASSWORD);

        // 锁定了，就提前返回，不再验证密码
        LockOutRealm lockOutRealm = getLockOutRealm(request);
        if (lockOutRealm.isLocked(user)) {
            long left = lockOutRealm.getLockOutTime() - (System.currentTimeMillis() - lockOutRealm.getLockRecord(user).getLastFailureTime()) / 1000 + 1;
            if (left < 0) {
                left = 0;
            }
            left = left / 60;
            String msgKey = LOCKED_MSG_KEY + "," + lockOutRealm.getFailureCount() + "," + (left == 0 ? (left + 1) : left);
            // login.jsp 已经在 application.xml 中配置了过滤，
            // 因此，不需要加：encodeRedirectURL，否则会在登录后的浏览器上显示出 csrf 的令牌值，反而有安全风险
            return new LoginFailedMsg(msgKey, null);
        }

        String checkPasswordError = checkPassword(user, password);
        boolean loginOk = checkPasswordError == null;

        if (!loginOk) {
            loginOk = checkOtp(request);
        }

        loginOk = lockOutRealm.filterLockedAccounts(user, loginOk);

        if (loginOk) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                logout(request);// logout old user
            }
            session = request.getSession(true);

            // 登录成功，注册信息
            setLoginUser(session, user);
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

    private static boolean checkOtp(HttpServletRequest request) throws Exception {
        return checkOtp(request.getParameter(LOGIN_USER),
                SystemController.decryptWithConsolePrivateKey(request.getParameter(LOGIN_OTP), false));
    }

    private static boolean checkOtp(String user, String inputOtp) throws Exception {
        if (inputOtp == null) {
            return false;
        }

        User u = SystemController.getConsole().getUser(user);
        if (u == null || !u.isEnableOtp()) {
            return false; // 用户未开启动态密码
        }

        String keyForOtp = u.getKeyForOtp();
        TotpCipher totpCipher = SystemController.getService(CryptoService.class).getTotpCipher();
        return totpCipher.verifyCode(keyForOtp, inputOtp);
    }

    private static String checkPassword(String user, String password) {
        try {
            password = SystemController.decryptWithConsolePrivateKey(password, true);
        } catch (Exception ignored) {
        }

        try {
            User u = SystemController.getConsole().getUser(user);
            if (u != null
                    && u.isActive()
                    && SystemController.getService(CryptoService.class).getMessageDigest().matches(password, u.getPassword())) {
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
            return "An error occurred in the password verification process: " + msg;
        }
    }

    public static void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public static String retrieveI18nMsg(String webLoginMsg) {
        String common_msg = webLoginMsg;
        if (common_msg != null) {
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
        String i18n = I18n.getKeyI18n(msg);
        return i18n != null ? i18n : msg;
    }

    public static boolean isOpenUris(String checkUri) {
        if (checkUri.startsWith("/static/")) {
            for (String suffix : STATIC_RES_SUFFIX) {
                if (checkUri.endsWith(suffix)) return true;
            }
        }

        // 远程实例注册
        String baseUri = DeployerConstants.REST_PREFIX + "/" + JsonView.FLAG + "/" + DeployerConstants.APP_SYSTEM + "/" + DeployerConstants.MODEL_MASTER + "/";
        return checkUri.equals(baseUri + DeployerConstants.ACTION_CHECK)
                ||
                checkUri.equals(baseUri + DeployerConstants.ACTION_REGISTER);
    }

    @Override
    public boolean doFilter(SystemControllerContext context) throws Exception {
        HttpServletRequest request = context.req;
        HttpServletResponse response = context.resp;
        String checkPath = RESTController.getReqUri(request);

        switch (checkPath) {
            // 登录
            case LOGIN_URI:
                try {
                    I18n.resetI18nLang(); // 确保登录页面（包括出错信息）都以默认i18n展示
                    webLogin(request, response);
                } finally {
                    I18n.resetI18nLang();
                }
                return false;


            // 注销
            case LOGIN_PATH:
                if (request.getParameter(LOGOUT_FLAG) != null) {
                    HttpSession session = request.getSession(false);
                    if (session != null) {
                        LoginManager.logout(request);
                    }
                }
                forwardToLoginJsp(request, response);
                return false;


            // 主页
            case "/":
                HttpSession session = request.getSession(false);
                if (session != null) {
                    response.sendRedirect(RESTController.encodeURL(response, request.getContextPath() + INDEX_PATH));
                } else {
                    forwardToLoginJsp(request, response);
                }
                return false;
        }

        // 已登录，进入业务系统
        String loginUser = getLoginUser(request);
        if (loginUser != null) return true;

        // 虽未登录，是开放资源，放行
        if (isOpenUris(checkPath)) return true;

        // 拦截未登录的请求
        if (!response.isCommitted()) {
            if (request.getHeader("accept") != null && request.getHeader("accept").contains("application/json")) {
                response.setContentType("application/json;charset=UTF-8");
                try (PrintWriter writer = context.resp.getWriter()) {
                    writer.write("{\"success\":\"false\",\"msg\":\"" + I18n.getKeyI18n("page.login.need") + "\"}");
                    writer.flush();
                }
            } else {
                String toJson = JsonView.responseErrorJson(response, "Please enter username and password to log in to the system");
                response.setHeader(RESPONSE_HEADER_MSG_KEY, toJson);
                response.sendRedirect(request.getContextPath() + LOGIN_PATH);
            }
        }
        return false;
    }

    public static void forwardToLoginJsp(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher(HtmlView.htmlPageBase + "login.jsp").forward(request, response);
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
