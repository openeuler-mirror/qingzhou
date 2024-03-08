package qingzhou.bootstrap.main.impl;

import qingzhou.bootstrap.Utils;
import qingzhou.bootstrap.main.FrameworkContext;
import qingzhou.bootstrap.main.service.ServiceManager;

import java.io.File;
import java.util.Map;

public class FrameworkContextImpl implements FrameworkContext {
    private final ServiceManagerImpl serviceManager = new ServiceManagerImpl();
    private Boolean masterNode;

    @Override
    public boolean isMaster() {
        if (masterNode == null) {
            Map<String, String> console = Utils.getAttributes(Utils.getServerXml(Utils.getDomain()), "//console");
            masterNode = Boolean.parseBoolean(console.get("enabled"));
        }
        return masterNode;
    }

    @Override
    public String getName() {
        return "Qingzhou（轻舟）";
    }

    @Override
    public String getVersion() {
        String versionFlag = "version";
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
        if (subName != null && !subName.trim().isEmpty()) {
            tmpdir = new File(tmpdir, subName.trim());
        }
        if (!tmpdir.exists()) {
            if (!tmpdir.mkdirs()) {
                throw new IllegalStateException("failed to mkdirs: " + tmpdir);
            }
        }
        return tmpdir;
    }

    @Override
    public File getDomain() {
        return Utils.getDomain();
    }

    @Override
    public File getLib() {
        return Utils.getLibDir();
    }

    @Override
    public ServiceManager getServiceManager() {
        return serviceManager;
    }
}
