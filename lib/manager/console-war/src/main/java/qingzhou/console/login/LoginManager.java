package qingzhou.console.login;

import qingzhou.config.User;
import qingzhou.console.controller.I18n;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.SystemControllerContext;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.login.method.OtpLogin;
import qingzhou.console.login.method.PwdLogin;
import qingzhou.console.view.type.HtmlView;
import qingzhou.console.view.type.JsonView;
import qingzhou.core.DeployerConstants;
import qingzhou.engine.util.pattern.Filter;
import qingzhou.logger.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

public class LoginManager implements Filter<SystemControllerContext> {
    public static final String LOGIN_PATH = "/login";
    public static final String LOGIN_URI = "/j_login";
    public static final String LOGOUT_FLAG = "invalidate";

    public static final String LOGGED_SESSION_KEY = "LOGGED_SESSION_KEY";
    public static final String LOGIN_USER = "j_username";
    public static final String LOGIN_PASSWORD = "j_password";
    public static final String LOGIN_OTP = "otp";
    private static final LockOutRealm lockOutRealm = new LockOutRealm();

    public static final String RESPONSE_HEADER_MSG_KEY = "HEADER_MSG_KEY";
    public static final String INDEX_PATH = DeployerConstants.REST_PREFIX + "/" + HtmlView.FLAG + "/" + DeployerConstants.APP_SYSTEM + "/" + DeployerConstants.MODEL_INDEX + "/" + DeployerConstants.ACTION_INDEX;
    private static final String[] STATIC_RES_SUFFIX = {".html", ".js", ".css", ".ico", ".jpg", ".png", ".gif", ".ttf", ".woff", ".eot", ".svg", ".pdf"};

    private static final String LOGIN_ERROR_MSG_KEY = "page.login.invalid";
    private static final String LOCKED_MSG_KEY = "page.login.locked";

    static {
        I18n.addKeyI18n(LOGIN_ERROR_MSG_KEY, new String[]{"登录失败，用户名或密码错误。当前登录失败 %s 次，连续失败 %s 次，账户将锁定", "en:Login failed, wrong username or password. The current login failed %s times, and the account will be locked after %s consecutive failures"});
        I18n.addKeyI18n(LOCKED_MSG_KEY, new String[]{"连续登录失败 %s 次，账户已经锁定，请 %s 分钟后重试", "en:Login failed %s times in a row, account is locked, please try again in %s minutes"});
    }

    private static final LoginMethod[] LOGIN_METHODS = {new PwdLogin(), new OtpLogin()};

    public static Object login(Parameter parameter) throws Exception {
        String user = parameter.get(LOGIN_USER);

        // 锁定了，就提前返回，不再验证密码
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

        User authorized = null;
        for (LoginMethod loginMethod : LOGIN_METHODS) {
            try {
                authorized = loginMethod.authorize(parameter);
            } catch (Throwable e) {
                SystemController.getService(Logger.class).error(e.getMessage(), e);
            }
            if (authorized != null) break;
        }

        boolean loginSuccess = authorized != null;
        loginSuccess = lockOutRealm.filterLockedAccounts(user, loginSuccess);
        if (!loginSuccess) {
            String msgKey = LOGIN_ERROR_MSG_KEY;
            int failureCount;
            LockOutRealm.LockRecord lockRecord = lockOutRealm.getLockRecord(user);
            if (lockRecord != null) {
                failureCount = lockRecord.getFailures();
                msgKey = LOGIN_ERROR_MSG_KEY + "," + failureCount + "," + lockOutRealm.getFailureCount();
            }
            return new LoginFailedMsg(msgKey, null);
        }

        return authorized;
    }

    public static void forwardToLoginJsp(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher(HtmlView.HTML_PAGE_BASE + "login.jsp").forward(request, response);
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

    public static User getLoggedUser(HttpSession session) {
        if (session == null) return null;
        try {
            return (User) session.getAttribute(LOGGED_SESSION_KEY);
        } catch (Exception e) { // 可能是登陆页面，在首行 注销了
            SystemController.getService(Logger.class).error(e.getMessage(), e);
            return null;
        }
    }

    public static void loginSession(HttpServletRequest request, User authorized) {
        HttpSession session = request.getSession(true);
        session.setAttribute(LOGGED_SESSION_KEY, authorized);
    }

    public static void logoutSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @Override
    public boolean doFilter(SystemControllerContext context) throws Exception {
        HttpServletRequest request = context.req;
        HttpServletResponse response = context.resp;
        HttpSession session = request.getSession(false);

        // 已登录，进入业务系统
        User loggedUser = getLoggedUser(session);
        if (loggedUser != null) return true;

        // 检查是否拦截要访问的资源
        String checkPath = RESTController.getReqUri(request);

        // 未登录，是开放资源，放行
        if (isOpenUris(checkPath)) return true;

        switch (checkPath) {
            case LOGIN_URI: // 登录
                try {
                    I18n.resetI18nLang(); // 确保登录页面（包括出错信息）都以默认i18n展示
                    webLogin(request, response);
                } finally {
                    I18n.resetI18nLang();
                }
                return false;
            case LOGIN_PATH: // 注销
                if (request.getParameter(LOGOUT_FLAG) != null) {
                    if (session != null) {
                        LoginManager.logoutSession(request);
                    }
                }
                forwardToLoginJsp(request, response);
                return false;
            case "/": // 主页
                if (session != null) {
                    response.sendRedirect(RESTController.encodeURL(response, request.getContextPath() + INDEX_PATH));
                } else {
                    forwardToLoginJsp(request, response);
                }
                return false;
        }

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
        return false; // 拦截未登录的请求
    }

    private void webLogin(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Object result = login(request::getParameter);
        if (result instanceof User) {
            LoginManager.loginSession(request, (User) result);
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
            LoginFailedMsg failedMsg = (LoginFailedMsg) result;
            String failedMsgKey = failedMsg.getHeaderMsg();
            String msg = LoginManager.retrieveI18nMsg(failedMsgKey);
            JsonView.responseErrorJson(response, msg);

            if (!response.isCommitted()) {
                String redirectUri = failedMsg.getRedirectUri();
                if (redirectUri == null) {
                    redirectUri = LOGIN_PATH + "?" + RESTController.MSG_FLAG + "=" + failedMsgKey;
                }

                response.sendRedirect(request.getContextPath() + redirectUri);
            }
        }
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
