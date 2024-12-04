package qingzhou.core;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface LoginInterceptor {

    Result login(HttpServletRequest request, HttpServletResponse response) throws Exception;

    default void afterLogin(HttpServletRequest request, HttpServletResponse response) throws Exception {
    }

    default Result logout(HttpServletRequest request, HttpServletResponse response) throws Exception {
        return null;
    }

    class Result {
        private String redirectUrl;
        private String username;

        public void setRedirectUrl(String redirectUrl) {
            this.redirectUrl = redirectUrl;
        }

        public void setUsername(String username) {
            this.username = username;
        }


        public String getRedirectUrl() {
            return redirectUrl;
        }

        public String getUsername() {
            return username;
        }
    }
}
