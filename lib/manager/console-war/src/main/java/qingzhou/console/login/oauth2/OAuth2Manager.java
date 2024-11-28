package qingzhou.console.login.oauth2;

import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.SystemControllerContext;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.login.LoginManager;
import qingzhou.core.config.User;
import qingzhou.engine.util.pattern.Filter;
import qingzhou.logger.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

public class OAuth2Manager implements Filter<SystemControllerContext> {
    private final String this_receive_code_path = "/oauth2/code_callback";
    private final String this_listen_logout_path = "/oauth2/logout_callback";
    private final String SESSION_TOKEN_FLAG = "SESSION_TOKEN_FLAG";
    private final OAuth2Client oAuth2Client;
    private final Map<String, HttpSession> tokenSessionsCache = new WeakHashMap<>(); // 为避免内存泄漏，用了 WeakHashMap，可能会导致会话清理通知失效（后续还可依赖本地失效机制）

    public OAuth2Manager() {
        OAuthConfig config = load();
        if (config == null) {
            oAuth2Client = null;
            return;
        }
        String serverUrl = config.getRedirectUrl();
        while (serverUrl.endsWith("/")) {
            serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
        }
        config.setListenLogout(serverUrl + this_listen_logout_path);
        config.setReceiveCodeUrl(serverUrl + this_receive_code_path);

        oAuth2Client = OAuth2Client.getInstance(config);
    }

    private OAuthConfig load() {
        Properties properties = new Properties();
        for (String name : System.getProperties().stringPropertyNames()) {
            String PROPERTIES_PREFIX = "oauth2.";
            if (name.startsWith(PROPERTIES_PREFIX)) {
                properties.put(name.substring(PROPERTIES_PREFIX.length()), System.getProperty(name));
            }
        }
        if (properties.isEmpty()) {
            return null;
        }
        return new OAuthConfig(properties);
    }

    @Override
    public boolean doFilter(SystemControllerContext context) throws Exception {
        if (oAuth2Client == null) return true;

        HttpServletRequest request = context.req;
        HttpServletResponse response = context.resp;

        if (request.getParameter(LoginManager.LOGOUT_FLAG) != null) {
            return doLocalLogout(request, response);
        }

        String checkPath = RESTController.getReqUri(request);
        if (LoginManager.isOpenUris(checkPath)) {
            return true;
        }

        if (checkPath.equals(this_listen_logout_path)) {
            return doOAuthServerLogout(request);
        }

        if (checkPath.equals(this_receive_code_path)) {
            return doReceiveCode(request, response);
        }

        String accessToken = request.getParameter("access_token");
        if (accessToken != null) { // 携带 token，直接登录
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
            return doRefreshToken(request);
        }

        // 去往认证中心的登录页面
        if (!response.isCommitted()) {
            response.sendRedirect(oAuth2Client.getLoginUrl());
        }
        return false;
    }

    private boolean doRefreshToken(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            //需要判断下SESSION_TOKEN_FLAG是否有值，否则在首次开启oauth2认证后会报token=null空指针，因为没有登陆session没有存入
            if (session.getAttribute(SESSION_TOKEN_FLAG) != null) { // 续期
                String token = (String) session.getAttribute(SESSION_TOKEN_FLAG);
                String SESSION_TOKEN_INTROSPECT_TIME = "SESSION_TOKEN_INTROSPECT_TIME";
                Long lastTime = (Long) session.getAttribute(SESSION_TOKEN_INTROSPECT_TIME);
                if (lastTime == null || (System.currentTimeMillis() - lastTime > 2 * 60 * 1000)) { // 简单实现，两次间隔 2 分钟
                    if (oAuth2Client.checkToken(token)) {
                        session.setAttribute(SESSION_TOKEN_INTROSPECT_TIME, System.currentTimeMillis());
                    } else {
                        LoginManager.logout(request);
                    }
                }
            }
        } catch (Exception e) {
            SystemController.getService(Logger.class).warn(e.getMessage(), e);
        }
        return true;
    }

    private boolean doReceiveCode(HttpServletRequest request, HttpServletResponse response) throws IOException {
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

    private boolean doOAuthServerLogout(HttpServletRequest request) {
        String token = request.getParameter("token");
        if (token != null && !token.isEmpty()) {
            HttpSession s = tokenSessionsCache.remove(token);
            if (s != null) {
                s.invalidate();
            }
        }
        return true;
    }

    private boolean doLocalLogout(HttpServletRequest request, HttpServletResponse response) {
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
