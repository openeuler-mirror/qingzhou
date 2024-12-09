package qingzhou.console.login;

import qingzhou.config.User;

public interface LoginMethod {
    User authorize(Parameter parameter) throws Throwable;
}
