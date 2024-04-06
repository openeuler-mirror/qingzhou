package qingzhou.command.server.config;


import qingzhou.command.CommandUtil;

public class Arg {
    private String name;
    private boolean onlyForLinux = false;
    private String supportedJRE;
    private String javaVersion;

    public Arg(String name) {
        this.name = name;
    }

    public Arg(String name, boolean onlyForLinux, String supportedJRE) {
        this.name = name;
        this.onlyForLinux = onlyForLinux;
        this.supportedJRE = supportedJRE;
    }

    public void prepare(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    public boolean available() {
        if (isOnlyForLinux()) {
            if (CommandUtil.isWindows()) {
                return false;
            }
        }

        if (CommandUtil.notBlank(supportedJRE)) {
            int jreVer;
            boolean minus = supportedJRE.endsWith("-");
            boolean plus = supportedJRE.endsWith("+");
            if (minus || plus) {
                jreVer = Integer.parseInt(supportedJRE.substring(0, supportedJRE.length() - 1));
            } else {
                jreVer = Integer.parseInt(supportedJRE);
            }

            String ver = System.getProperty("java.specification.version");
            if (this.javaVersion != null) {
                ver = this.javaVersion;
            }
            if (ver != null) {
                int currentJreVer = CommandUtil.parseJavaVersion(ver);
                if (minus) {
                    if (currentJreVer > jreVer) {
                        return false;
                    }
                }
                if (plus) {
                    if (currentJreVer < jreVer) {
                        return false;
                    }
                }
                if (!minus && !plus) {
                    return currentJreVer == jreVer;
                }
            }
        }
        return true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isOnlyForLinux() {
        return onlyForLinux;
    }
}
