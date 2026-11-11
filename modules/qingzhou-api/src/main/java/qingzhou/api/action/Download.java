package qingzhou.api.action;

import java.io.File;

public interface Download {
    String ACTION_CODE_FILE = "file";
    String ACTION_CODE_DOWNLOAD = "download";

    // 下载文件的父目录或文件本身，安全考虑：子目录会被忽略
    File file(String id) throws Exception;
}