package qingzhou.config;

public class Security {
    private String publicKey;
    private String privateKey;
    private String trustedIp;
    private boolean verCodeEnabled;
    private int lockOutTime;
    private int failureCount;
    private int passwordMaxAge;
    private int passwordLimitRepeats;
    private int maxFileUpload;

    private boolean enabledOAuth2;
    private String clientId;
    private String clientSecret;
    private String redirectUrl;
    private String authorizeUrl;
    private String tokenUrl;
    private String userInfoUrl;
    private String checkTokenUrl;
    private String logoutUrl;

    public int getMaxFileUpload() {
        return maxFileUpload;
    }

    public void setMaxFileUpload(int maxFileUpload) {
        this.maxFileUpload = maxFileUpload;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public int getPasswordMaxAge() {
        return passwordMaxAge;
    }

    public void setPasswordMaxAge(int passwordMaxAge) {
        this.passwordMaxAge = passwordMaxAge;
    }

    public int getPasswordLimitRepeats() {
        return passwordLimitRepeats;
    }

    public void setPasswordLimitRepeats(int passwordLimitRepeats) {
        this.passwordLimitRepeats = passwordLimitRepeats;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public int getLockOutTime() {
        return lockOutTime;
    }

    public void setLockOutTime(int lockOutTime) {
        this.lockOutTime = lockOutTime;
    }

    public String getTrustedIp() {
        return trustedIp;
    }

    public void setTrustedIp(String trustedIp) {
        this.trustedIp = trustedIp;
    }

    public boolean isVerCodeEnabled() {
        return verCodeEnabled;
    }

    public void setVerCodeEnabled(boolean verCodeEnabled) {
        this.verCodeEnabled = verCodeEnabled;
    }

    public boolean isEnabledOAuth2() {
        return enabledOAuth2;
    }

    public void setEnabledOAuth2(boolean enabledOAuth2) {
        this.enabledOAuth2 = enabledOAuth2;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getAuthorizeUrl() {
        return authorizeUrl;
    }

    public void setAuthorizeUrl(String authorizeUrl) {
        this.authorizeUrl = authorizeUrl;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public String getUserInfoUrl() {
        return userInfoUrl;
    }

    public void setUserInfoUrl(String userInfoUrl) {
        this.userInfoUrl = userInfoUrl;
    }

    public String getCheckTokenUrl() {
        return checkTokenUrl;
    }

    public void setCheckTokenUrl(String checkTokenUrl) {
        this.checkTokenUrl = checkTokenUrl;
    }

    public String getLogoutUrl() {
        return logoutUrl;
    }

    public void setLogoutUrl(String logoutUrl) {
        this.logoutUrl = logoutUrl;
    }
}
