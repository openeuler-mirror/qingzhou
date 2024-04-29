package qingzhou.config;

public class User {
    private String id;
    private String info;
    private String password;
    private boolean active;
    private boolean changeInitPwd;
    private boolean enable2FA;
    private String keyFor2FA;
    private String passwordLastModifiedTime;

    public String getPasswordLastModifiedTime() {
        return passwordLastModifiedTime;
    }

    public void setPasswordLastModifiedTime(String passwordLastModifiedTime) {
        this.passwordLastModifiedTime = passwordLastModifiedTime;
    }

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

    public boolean isChangeInitPwd() {
        return changeInitPwd;
    }

    public void setChangeInitPwd(boolean changeInitPwd) {
        this.changeInitPwd = changeInitPwd;
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
}
