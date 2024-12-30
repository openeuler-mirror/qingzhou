package qingzhou.console.login;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import qingzhou.api.AuthAdapter;
import qingzhou.config.console.User;
import qingzhou.console.controller.rest.RESTController;

public class AuthContextImpl implements AuthAdapter.AuthContext {
    private final Parameter parameter;
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private boolean loginSuccessful;

    public AuthContextImpl(Parameter parameter, HttpServletRequest request, HttpServletResponse response) {
        this.parameter = parameter;
        this.request = request;
        this.response = response;
    }

    @Override
    public String getParameter(String name) {
        return parameter.get(name);
    }

    public boolean isLoginSuccessful() {
        return loginSuccessful;
    }

    @Override
    public void setLoginSuccessful(String user, String role) {
        LoginManager.loginSession(request, buildUser(user, role));
        // 进入主页
        try {
            response.sendRedirect(RESTController.encodeURL(response, request.getContextPath() + LoginManager.INDEX_PATH)); // to welcome page
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        loginSuccessful = true;
    }

    @Override
    public void redirect(String url) {
        try {
            response.sendRedirect(url);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setContentType(String contentType) {
        response.setContentType(contentType);
    }

    @Override
    public void setHeader(String name, String value) {
        response.setHeader(name, value);
    }

    @Override
    public void setHttpBody(byte[] body) throws IOException {
        response.getOutputStream().write(body);
    }

    private User buildUser(String username, String role) {
        User user = new User();
        user.setRole(role);
        user.setName(username);
        user.setActive(true);
        user.setChangePwd(false);
        user.setEnableOtp(false);
        return user;
    }
}
