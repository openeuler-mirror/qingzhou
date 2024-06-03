package qingzhou.config;


public class Arg {
    private boolean enabled = true;
    private String name;
    private String desc;
    private boolean forLinux = false;

    private String supportedJRE;

    public Arg() {
    }

    public Arg(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public boolean isForLinux() {
        return forLinux;
    }

    public void setForLinux(boolean forLinux) {
        this.forLinux = forLinux;
    }

    public String getSupportedJRE() {
        return supportedJRE;
    }

    public void setSupportedJRE(String supportedJRE) {
        this.supportedJRE = supportedJRE;
    }
}
