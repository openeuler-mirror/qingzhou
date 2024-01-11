package qingzhou.framework.impl;

import qingzhou.framework.AppManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.util.FileUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FrameworkContextImpl implements FrameworkContext {
    private static File libDir;
    private static File home;
    private static File domain;

    static FrameworkContextImpl frameworkContext;

    public static FrameworkContextImpl getFrameworkContext() {
        return frameworkContext;
    }

    private final AppManagerImpl appInfoManager = new AppManagerImpl();
    private final Map<Class<?>, Object> services = new HashMap<>();

    @Override
    public AppManager getAppManager() {
        return appInfoManager;
    }

    @Override
    public <T> void registerService(Class<T> clazz, T service) {
        if (services.containsKey(clazz)) {
            throw new IllegalArgumentException();
        }
        services.put(clazz, service);
    }

    @Override
    public <T> T getService(Class<T> serviceType) {
        return (T) services.get(serviceType);
    }

    @Override
    public File getHome() {
        if (home == null) {
            home = new File(System.getProperty("qingzhou.home"));
        }
        return home;
    }

    @Override
    public File getDomain() {
        if (FrameworkContextImpl.domain == null) {
            String domainName = System.getProperty("qingzhou.domain");
            if (domainName == null || domainName.trim().isEmpty()) {
                throw new NullPointerException("qingzhou.domain");
            }
            FrameworkContextImpl.domain = new File(domainName).getAbsoluteFile();
        }
        return FrameworkContextImpl.domain;
    }

    @Override
    public File getCache() {
        File temp = new File(getTempDir(), "cache");
        return getCache(temp);
    }

    @Override
    public File getCache(File parent) {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String timeFlag = df.format(new Date());
        File result = new File(parent, timeFlag);
        FileUtil.mkdirs(result);
        return result;
    }

    private File getTempDir() {
        File tmpdir;
        File domain = getDomain();
        if (domain != null) {
            tmpdir = new File(domain, "temp");
        } else {
            tmpdir = new File(System.getProperty("java.io.tmpdir"));
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

    @Override
    public Set<Class<?>> getServiceTypes() {
        return services.keySet();
    }
}
