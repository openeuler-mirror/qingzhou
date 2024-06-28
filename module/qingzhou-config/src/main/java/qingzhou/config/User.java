package qingzhou.config;

public class User {
    private String id;
    private String info;
    private String password;
    private boolean active;
    private boolean changePwd;
    private boolean enable2FA;
    private String keyFor2FA;
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

    public boolean isEnable2FA() {
        return enable2FA;
    }

    public void setEnable2FA(boolean enable2FA) {
        this.enable2FA = enable2FA;
    }

    public String getKeyFor2FA() {
        return keyFor2FA;
    }

    public void setKeyFor2FA(String keyFor2FA) {
        this.keyFor2FA = keyFor2FA;
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
