package qingzhou.framework;

import java.io.File;

public interface FileManager {
    File getTemp(String subName);

    File getDomain();

    File getHome();

    File getLib();
}
