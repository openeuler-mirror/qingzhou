package qingzhou.config;

public class User {
    private String id;
    private String info;
    private String password;
    private boolean active;
    private boolean changePwd;
    private boolean enableOtp;
    private String keyForOtp;
    private String passwordLastModified;
    private String historyPasswords;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isChangePwd() {
        return changePwd;
    }

    public void setChangePwd(boolean changePwd) {
        this.changePwd = changePwd;
    }

    public boolean isEnableOtp() {
        return enableOtp;
    }

    public void setEnableOtp(boolean enableOtp) {
        this.enableOtp = enableOtp;
    }

    public String getKeyForOtp() {
        return keyForOtp;
    }

    public void setKeyForOtp(String keyForOtp) {
        this.keyForOtp = keyForOtp;
    }

    public String getHistoryPasswords() {
        return historyPasswords;
    }

    public void setHistoryPasswords(String historyPasswords) {
        this.historyPasswords = historyPasswords;
    }

    public String getPasswordLastModified() {
        return passwordLastModified;
    }

    public void setPasswordLastModified(String passwordLastModified) {
        this.passwordLastModified = passwordLastModified;
    }
}
