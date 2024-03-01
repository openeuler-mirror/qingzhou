package qingzhou.bootstrap.main;

import qingzhou.bootstrap.main.service.ServiceManager;

import java.io.File;

public interface FrameworkContext {
    //  Qingzhou 产品名词
    String getName();

    // 产品版本信息
    String getVersion();

    File getDomain();

    File getTemp(String subName);

    File getLib();

    ServiceManager getServiceManager();
}
