package qingzhou.engine.impl;

import qingzhou.engine.Main;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.RegistryKey;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

public class ModuleContextImpl implements ModuleContext {
    private final ServiceManagerImpl serviceManager = new ServiceManagerImpl();
    private File libDir;
    private File instanceDir;

    @Override
    public String getName() {
        return "Qingzhou（轻舟）";
    }

    @Override
    public String getVersion() {
        String versionFlag = "version";
        return getLibDir().getName().substring(versionFlag.length());
    }

    @Override
    public File getLibDir() {
        if (libDir == null) {
            String jarPath = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            String flag = "/engine/qingzhou-engine.jar";
            int i = jarPath.indexOf(flag);
            String pre = jarPath.substring(0, i);
            libDir = new File(pre);
        }
        return libDir;
    }

    @Override
    public <T> RegistryKey registerService(Class<T> clazz, T service) {
        return serviceManager.registerService(clazz, service);
    }

    @Override
    public void unregisterService(RegistryKey registryKey) {
        serviceManager.unregisterService(registryKey);
    }

    @Override
    public Collection<Class<?>> getServiceTypes() {
        return serviceManager.getServiceTypes();
    }

    @Override
    public <T> T getService(Class<T> serviceType) {
        return serviceManager.getService(serviceType);
    }

    @Override
    public File getInstanceDir() {
        if (instanceDir == null) {
            String instance = System.getProperty("qingzhou.instance");
            if (instance == null || instance.trim().isEmpty()) {
                throw new IllegalArgumentException();// 不要在这里设置 instance1，应该在调用端去捕捉异常并处理
            }
            try {
                this.instanceDir = new File(instance).getCanonicalFile();
            } catch (IOException e) {
                throw new IllegalArgumentException();// 不要在这里设置 instance1，应该在调用端去捕捉异常并处理
            }
        }
        return instanceDir;
    }

    @Override
    public File getTemp() {
        File temp = new File(getInstanceDir(), "temp");
        return new File(temp, UUID.randomUUID().toString());
    }
}
