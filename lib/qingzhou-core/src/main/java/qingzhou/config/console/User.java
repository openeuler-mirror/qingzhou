package qingzhou.config.console;

public class User {
    private String name;
    private String info;
    private String password;
    private boolean active;
    private boolean changePwd;
    private boolean enableOtp;
    private String keyForOtp;
    private String passwordLastModified;
    private String historyPasswords;
    private String role;
    private String type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
