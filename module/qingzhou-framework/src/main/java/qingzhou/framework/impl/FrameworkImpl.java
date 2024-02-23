package qingzhou.framework.impl;

import qingzhou.framework.Framework;
import qingzhou.framework.util.FileUtil;
import qingzhou.framework.util.StringUtil;

import java.io.File;

public class FrameworkImpl implements Framework {
    private final String versionFlag = "version";
    private File home;
    private File lib;
    private File domain;

    @Override
    public String getName() {
        return "Qingzhou（轻舟）";
    }

    @Override
    public String getVersion() {
        return getLib().getName().substring(versionFlag.length());
    }

    @Override
    public File getTemp(String subName) {
        File tmpdir;
        File domain = getDomain();
        if (domain != null) {
            tmpdir = new File(domain, "temp");
        } else {
            tmpdir = new File(System.getProperty("java.io.tmpdir"));
        }
        if (StringUtil.notBlank(subName)) {
            tmpdir = new File(tmpdir, subName);
        }
        FileUtil.mkdirs(tmpdir);
        return tmpdir;
    }

    @Override
    public File getDomain() {
        if (domain == null) {
            String domainName = System.getProperty("qingzhou.domain");
            if (domainName == null || domainName.trim().isEmpty()) {
                throw new NullPointerException("qingzhou.domain");
            }
            domain = new File(domainName).getAbsoluteFile();
        }
        return domain;
    }

    @Override
    public File getLib() {
        if (lib == null) {
            String jarPath = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
            String flag = "/" + versionFlag;
            int i = jarPath.lastIndexOf(flag);
            int j = jarPath.indexOf("/", i + flag.length());
            lib = new File(new File(getHome(), "lib"), jarPath.substring(i + 1, j));
        }
        return lib;
    }

    private File getHome() {
        if (home == null) {
            home = new File(System.getProperty("qingzhou.home"));
        }
        return home;
    }
}
