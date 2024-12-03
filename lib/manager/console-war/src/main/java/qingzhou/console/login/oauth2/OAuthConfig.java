package qingzhou.console.login.oauth2;

import java.util.Map;

class OAuthConfig {
        private boolean enabled;
        private String redirectUrl;     // 当前应用域名
        private String clientId;
        private String clientSecret;
        private String authorizeUrl;    // 授权地址
        private String tokenUrl;        // 获取token地址
        private String checkTokenUrl;   // 检验token地址
        private String userInfoUrl;         // 获取用户信息地址
        private String logoutUrl;       // 注销地址
        private String listenLogout;    // 注销回调地址
        private String receiveCodeUrl;
        private String serverVendor;

        public OAuthConfig(Map<String, String> properties) {
            this.enabled = Boolean.parseBoolean(properties.get("enabled"));
            this.redirectUrl = properties.get("redirectUrl");
            this.clientId = properties.get("clientId");
            this.clientSecret = properties.get("clientSecret");
            this.authorizeUrl = properties.get("authorizeUrl");
            this.tokenUrl = properties.get("tokenUrl");
            this.checkTokenUrl = properties.get("checkTokenUrl");
            this.userInfoUrl = properties.get("userInfoUrl");
            this.logoutUrl = properties.get("logoutUrl");
            this.serverVendor = properties.get("serverVendor");
        }

        public boolean isEnabled() {
            return enabled;
        }

        public String getAuthorizeUrl() {
            return authorizeUrl;
        }

        public String getClientId() {
            return clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public String getCheckTokenUrl() {
            return checkTokenUrl;
        }

        public String getLogoutUrl() {
            return logoutUrl;
        }

        public String getTokenUrl() {
            return tokenUrl;
        }

        public String getRedirectUrl() {
            return redirectUrl;
        }

        public String getUserInfoUrl() {
            return userInfoUrl;
        }

        public String getServerVendor() {
            return serverVendor;
        }

        public String getListenLogout() {
            return listenLogout;
        }

        public void setListenLogout(String listenLogout) {
            this.listenLogout = listenLogout;
        }

        public String getReceiveCodeUrl() {
            return receiveCodeUrl;
        }

        public void setReceiveCodeUrl(String receiveCodeUrl) {
            this.receiveCodeUrl = receiveCodeUrl;
        }
    }