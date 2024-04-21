package qingzhou.config;

public class Security {
    private String trustedIP;
    private boolean verCodeEnabled;

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
