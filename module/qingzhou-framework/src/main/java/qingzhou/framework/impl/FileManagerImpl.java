package qingzhou.framework.impl;

import qingzhou.framework.FileManager;
import qingzhou.framework.util.FileUtil;
import qingzhou.framework.util.StringUtil;

import java.io.File;

public class FileManagerImpl implements FileManager {
    private File libDir;
    private File home;
    private File domain;

    @Override
    public File getHome() {
        if (home == null) {
            home = new File(System.getProperty("qingzhou.home"));
        }
        return home;
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
    public File getLib() {
        if (libDir == null) {
            String jarPath = FrameworkContextImpl.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            String flag = "/version";
            int i = jarPath.lastIndexOf(flag);
            int j = jarPath.indexOf("/", i + flag.length());
            libDir = new File(new File(getHome(), "lib"), jarPath.substring(i + 1, j));
        }
        return libDir;
    }
}
