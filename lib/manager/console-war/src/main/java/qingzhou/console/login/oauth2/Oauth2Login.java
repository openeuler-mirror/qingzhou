package qingzhou.console.login.oauth2;

import qingzhou.console.controller.SystemController;
import qingzhou.core.LoginInterceptor;
import qingzhou.logger.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

public class Oauth2Login implements LoginInterceptor {
    private final String this_receive_code_path = "/oauth2/code_callback";
    private final String this_listen_logout_path = "/oauth2/logout_callback";
    private final String SESSION_TOKEN_FLAG = "SESSION_TOKEN_FLAG";
    private final OAuth2Client oAuth2Client;
    private final Map<String, HttpSession> tokenSessionsCache = new WeakHashMap<>(); // 为避免内存泄漏，用了 WeakHashMap，可能会导致会话清理通知失效（后续还可依赖本地失效机制）
    private final ThreadLocal<String> tokenCache = new ThreadLocal<>();

    public Oauth2Login() {
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
    public Result login(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (oAuth2Client == null) return null;

        String checkPath = getReqUri(request);
        if (checkPath.equals(this_listen_logout_path)) {
            doOAuthServerLogout(request);
            return null;
        }

        if (checkPath.equals(this_receive_code_path)) {
            return doReceiveCode(request);
        }

        String accessToken = request.getParameter("access_token");
        if (accessToken != null) { // 携带 token，直接登录
            return getUserInfo(accessToken);
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            String token = (String) session.getAttribute(SESSION_TOKEN_FLAG);
            if (token != null) {
                return doRefreshToken(request);
            }
        }

        // 去往认证中心的登录页面
        Result result = new Result();
        result.setRedirectUrl(oAuth2Client.getLoginUrl());
        return result;
    }

    private Result getUserInfo(String accessToken) {
        Result result = new Result();
        try {
            String username = oAuth2Client.getUserInfo(accessToken);
            result.setUsername(username);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Result logout(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String token = (String) session.getAttribute(SESSION_TOKEN_FLAG);
            if (token != null) {
                tokenSessionsCache.remove(token);
                if (oAuth2Client.logout(token)) {
                    Result result = new Result();
                    result.setRedirectUrl(oAuth2Client.getLoginUrl());
                    return result;
                }
            }
        }

        return null;
    }

    @Override
    public void afterLogin(HttpServletRequest request, HttpServletResponse response) {
        String token = tokenCache.get();
        if (token != null) {
            HttpSession session = request.getSession(false);
            session.setAttribute(SESSION_TOKEN_FLAG, token);
            tokenSessionsCache.put(token, session);
            tokenCache.remove();
        }
    }

    private void doOAuthServerLogout(HttpServletRequest request) {
        String token = request.getParameter("token");
        if (token != null && !token.isEmpty()) {
            HttpSession s = tokenSessionsCache.remove(token);
            if (s != null) {
                s.invalidate();
            }
        }
    }

    private Result doReceiveCode(HttpServletRequest request) {
        String code = request.getParameter("code");
        try {
            String accessToken = oAuth2Client.login(code);
            if (accessToken != null) {
                tokenCache.set(accessToken);
                String username = oAuth2Client.getUserInfo(accessToken);
                Result result = new Result();
                result.setUsername(username);
                return result;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private Result doRefreshToken(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session != null) {
                String token = (String) session.getAttribute(SESSION_TOKEN_FLAG);
                String SESSION_TOKEN_INTROSPECT_TIME = "SESSION_TOKEN_INTROSPECT_TIME";
                Long lastTime = (Long) session.getAttribute(SESSION_TOKEN_INTROSPECT_TIME);
                if (lastTime == null || (System.currentTimeMillis() - lastTime > 2 * 60 * 1000)) { // 简单实现，两次间隔 2 分钟
                    if (oAuth2Client.checkToken(token)) {
                        session.setAttribute(SESSION_TOKEN_INTROSPECT_TIME, System.currentTimeMillis());
                    } else {
                        Result result = new Result();
                        result.setRedirectUrl(oAuth2Client.getLoginUrl());
                        return result;
                    }
                }
            }
        } catch (Exception e) {
            SystemController.getService(Logger.class).warn(e.getMessage(), e);
        }
        return null;
    }

    public static String getReqUri(HttpServletRequest request) {
        return request.getServletPath() + (request.getPathInfo() != null ? request.getPathInfo() : "");
    }
}
