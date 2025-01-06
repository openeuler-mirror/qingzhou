package qingzhou.console.login;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import qingzhou.api.AuthAdapter;
import qingzhou.config.console.User;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.core.DeployerConstants;

public class AuthContextImpl implements AuthAdapter.AuthContext {
    private final Parameter parameter;
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    boolean loginSuccessful;

    public AuthContextImpl(Parameter parameter, HttpServletRequest request, HttpServletResponse response) {
        this.parameter = parameter;
        this.request = request;
        this.response = response;
    }

    @Override
    public String getParameter(String name) {
        return parameter.get(name);
    }

    @Override
    public void login(String user, String... role) {
        User loginUser = buildUser(user, role);
        LoginManager.loginSession(request, loginUser);
        // 进入主页
        try {
            response.sendRedirect(RESTController.encodeURL(response, request.getContextPath() + LoginManager.INDEX_PATH)); // to welcome page
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        loginSuccessful = true;
    }

    @Override
    public void logout() {
        LoginManager.logoutSession(request);
        loginSuccessful = false;
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
