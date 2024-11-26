package qingzhou.console.login;

import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.SystemControllerContext;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.core.config.User;
import qingzhou.engine.util.pattern.Filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class OAuth2Manager implements Filter<SystemControllerContext> {
    private static final String this_receive_code_path = "/oauth2/code_callback";
    private static final String this_listen_logout_path = "/oauth2/logout_callback";
    public static final String SESSION_TOKEN_FLAG = "SESSION_TOKEN_FLAG";
    private static final String SESSION_TOKEN_INTROSPECT_TIME = "SESSION_TOKEN_INTROSPECT_TIME";
    private static OAuth2Client oAuth2Client;
    private final Map<String, HttpSession> tokenSessionsCache = new ConcurrentHashMap<>();
    public static final String PROPERTIES_PREFIX = "oauth2.";

    private static final OAuth2Client.OAuthConfig CONFIG = load();

    public static OAuth2Client.OAuthConfig load() {
        Properties properties = new Properties();
        for (String name : System.getProperties().stringPropertyNames()) {
            if (name.startsWith(PROPERTIES_PREFIX)) {
                properties.put(name.substring(PROPERTIES_PREFIX.length()), System.getProperty(name));
            }
        }
        return new OAuth2Client.OAuthConfig(properties);
    }

    public OAuth2Manager() {
        oAuth2Client = OAuth2Client.getInstance(CONFIG);
        if (oAuth2Client != null) {
            String serverUrl = oAuth2Client.getConfig().getRedirectUrl();
            while (serverUrl.endsWith("/")) {
                serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
            }
            CONFIG.setListenLogout(serverUrl + this_listen_logout_path);
            CONFIG.setReceiveCodeUrl(serverUrl + this_receive_code_path);
        }
    }

    public static boolean isOAuth2Enabled() {
        return oAuth2Client != null;
    }

    @Override
    public boolean doFilter(SystemControllerContext context) throws Exception {
        if (!isOAuth2Enabled()) {
            return true;
        }
        HttpServletRequest request = context.req;
        HttpServletResponse response = context.resp;

        if (request.getParameter(LoginManager.LOGOUT_FLAG) != null) {
            String token = null;
            HttpSession session = request.getSession(false);
            if (session != null) {
                token = (String) session.getAttribute(SESSION_TOKEN_FLAG);
                String sessionToken = null;
                for (Map.Entry<String, HttpSession> entry : tokenSessionsCache.entrySet()) {
                    if (entry.getValue() == session) {
                        sessionToken = entry.getKey();
                    }
                }
                if (sessionToken != null) {
                    tokenSessionsCache.remove(sessionToken);
                }
                LoginManager.logout(request);
            }

            try {
                if (oAuth2Client.logout(token)) {
                    response.sendRedirect(oAuth2Client.getLoginUrl());
                    return false;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            throw new IllegalStateException();
        }

        String checkPath = RESTController.getReqUri(request);
        if (LoginManager.isOpenUris(checkPath)) return true;

        if (checkPath.equals(this_listen_logout_path)) {
            String token = request.getParameter("token");
            if (token != null && !token.isEmpty()) {
                HttpSession s = tokenSessionsCache.remove(token);
                if (s != null) {
                    s.invalidate();
                }
            }
            return true;
        }
        if (checkPath.equals(this_receive_code_path)) {
            String[] loginInfo;
            String code = request.getParameter("code");
            String uid;
            try {
                loginInfo = oAuth2Client.login(code);
                uid = loginInfo[1];
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (uid != null) {
                // 登录成功，注册信息，授予权限
                loginInternal(loginInfo[0], uid, request);

                // 进入主页
                response.sendRedirect(RESTController.encodeURL(response, request.getContextPath() + LoginManager.INDEX_PATH));
            } else {
                String msg = "Login failed";
                response.setHeader(LoginManager.RESPONSE_HEADER_MSG_KEY, msg);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, msg);
            }
            return false;
        }

        String accessToken = request.getParameter("access_token");
        if (accessToken != null) {
            String uid;
            try {
                uid = oAuth2Client.getUserInfo(accessToken);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (uid != null) {
                // 登录成功，注册信息，授予权限
                loginInternal(accessToken, uid, request);
                // 进入主页
                response.sendRedirect(RESTController.encodeURL(response, request.getContextPath() + LoginManager.INDEX_PATH)); // to welcome page
                return false;
            }
        }

        if (LoginManager.getLoginUser(request) != null) {
            try {
                HttpSession session = request.getSession(false);
                //需要判断下SESSION_TOKEN_FLAG是否有值，否则在首次开启oauth2认证后会报token=null空指针，因为没有登陆session没有存入
                if (session.getAttribute(SESSION_TOKEN_FLAG) != null) {
                    refresh(request); // 续期
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }

        // 去往认证中心的登录页面
        if (!response.isCommitted()) {
            response.sendRedirect(oAuth2Client.getLoginUrl());
        }
        return false;
    }


    private void refresh(HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String token = (String) session.getAttribute(SESSION_TOKEN_FLAG);
            Long lastTime = (Long) session.getAttribute(SESSION_TOKEN_INTROSPECT_TIME);
            if (lastTime == null || (System.currentTimeMillis() - lastTime > 2 * 60 * 1000)) { // 简单实现，两次间隔 2 分钟
                if (oAuth2Client.checkToken(token)) {
                    session.setAttribute(SESSION_TOKEN_INTROSPECT_TIME, System.currentTimeMillis());
                } else {
                    LoginManager.logout(request);
                }
            }
        }
    }

    private void loginInternal(String token, String uid, HttpServletRequest request) {
        mergeUserInfoAndRole(uid);
        LoginManager.loginOk(request, uid);
        HttpSession session = request.getSession(false);
        session.setAttribute(SESSION_TOKEN_FLAG, token);
        tokenSessionsCache.put(token, session);
    }

    // 加入原系统内部的验证机制
    private void mergeUserInfoAndRole(String username) {
        User user = new User();
        user.setName(username);
        user.setInfo(username);
        user.setActive(true);
        user.setChangePwd(false);
        user.setEnableOtp(false);

        // 并入内部的权限体系
        SystemController.getConsole().getOauthUsers().add(user);
    }


}
