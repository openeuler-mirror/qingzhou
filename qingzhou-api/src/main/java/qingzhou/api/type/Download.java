package qingzhou.api.type;

import java.io.File;

/**
 * 定义了与下载相关的操作。
 */
public interface Download {
    String ACTION_FILES = "files";
    String ACTION_DOWNLOAD = "download";

    File downloadData(String id) throws Exception;
}