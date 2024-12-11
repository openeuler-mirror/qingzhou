package qingzhou.console.login;

import qingzhou.config.console.User;

public interface LoginMethod {
    User authorize(Parameter parameter) throws Throwable;
}
