package qingzhou.api;

import java.io.File;
import java.util.Properties;

public interface BasicContext {
    // 平台框架版本
    String getVersion();

    // 应用配置属性
    Properties getProperties();

    // 平台实例的根目录
    File getBase();

    // 应用专属的临时目录
    File getTemp();
}
