package qingzhou.config;

public class Security {
    private String publicKey;
    private String privateKey;
    private String trustedIP;
    private boolean verCodeEnabled;
    private int lockOutTime;
    private int failureCount;
    private boolean enablePasswordAge;
    private int passwordMaxAge;
    private int passwordMinAge;

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

    public boolean isEnablePasswordAge() {
        return enablePasswordAge;
    }

    public void setEnablePasswordAge(boolean enablePasswordAge) {
        this.enablePasswordAge = enablePasswordAge;
    }

    public int getPasswordMaxAge() {
        return passwordMaxAge;
    }

    public void setPasswordMaxAge(int passwordMaxAge) {
        this.passwordMaxAge = passwordMaxAge;
    }

    public int getPasswordMinAge() {
        return passwordMinAge;
    }

    public void setPasswordMinAge(int passwordMinAge) {
        this.passwordMinAge = passwordMinAge;
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

    public String getTrustedIP() {
        return trustedIP;
    }

    public void setTrustedIP(String trustedIP) {
        this.trustedIP = trustedIP;
    }

    public boolean isVerCodeEnabled() {
        return verCodeEnabled;
    }

    public void setVerCodeEnabled(boolean verCodeEnabled) {
        this.verCodeEnabled = verCodeEnabled;
    }
}
