package qingzhou.framework;

import java.io.File;

public interface Framework {
    // 轻舟产品名词
    String getName();

    // 产品版本信息
    String getVersion();

    File getDomain();

    File getTemp(String subName);

    File getLib();
}
