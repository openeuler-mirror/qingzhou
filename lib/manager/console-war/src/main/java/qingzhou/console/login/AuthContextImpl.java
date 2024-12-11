package qingzhou.console.login;

import qingzhou.api.AuthAdapter;
import qingzhou.config.console.User;
import qingzhou.console.controller.rest.RESTController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
    public void setLoginSuccessful(String user) throws IOException {
        LoginManager.loginSession(request, buildUser(user));
        // 进入主页
        response.sendRedirect(RESTController.encodeURL(response, request.getContextPath() + LoginManager.INDEX_PATH)); // to welcome page
        loginSuccessful = true;
    }

    @Override
    public void redirect(String url) throws IOException {
        response.sendRedirect(url);
    }

    private User buildUser(String username) {
        User user = new User();
        user.setName(username);
        user.setActive(true);
        user.setChangePwd(false);
        user.setEnableOtp(false);
        return user;
    }
}
