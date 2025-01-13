package qingzhou.console.login;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import qingzhou.api.AuthAdapter;
import qingzhou.config.console.User;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.core.DeployerConstants;

public class AuthContextImpl implements AuthAdapter.AuthContext {
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public AuthContextImpl(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    @Override
    public String getParameter(String name) {
        return request.getParameter(name);
    }

    @Override
    public void loggedIn(String user, String... role) throws IOException {
        User loginUser = buildUser(user, role);
        LoginManager.loginSession(request, loginUser);
        // 进入主页
        response.sendRedirect(RESTController.encodeURL(response, request.getContextPath() + LoginManager.INDEX_PATH)); // to welcome page
    }

    @Override
    public boolean isLoggedIn() {
        return LoginManager.getLoggedUser(request.getSession(false)) != null;
    }

    @Override
    public void redirect(String url) throws IOException {
        response.sendRedirect(url);
    }

    @Override
    public void responseContentType(String contentType) {
        response.setContentType(contentType);
    }

    @Override
    public void responseHeader(String name, String value) {
        response.setHeader(name, value);
    }

    @Override
    public void responseBody(byte[] body) throws IOException {
        response.getOutputStream().write(body);
    }

    private User buildUser(String username, String... role) {
        User user = new User();
        String roles = String.join(DeployerConstants.USER_ROLE_SP, role);
        user.setRole(roles);
        user.setName(username);
        user.setActive(true);
        user.setChangePwd(false);
        user.setEnableOtp(false);
        return user;
    }
}
