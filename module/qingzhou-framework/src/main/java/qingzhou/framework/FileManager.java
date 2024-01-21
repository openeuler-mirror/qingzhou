package qingzhou.framework;

import java.io.File;

public interface FileManager {
    File getCache();

    File getCache(File parent);

    File getDomain();

    File getHome();

    File getLib();
}
