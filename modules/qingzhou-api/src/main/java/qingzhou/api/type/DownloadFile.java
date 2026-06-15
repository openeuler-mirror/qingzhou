package qingzhou.api.type;

import java.io.File;

import qingzhou.api.QingzhouModel;

public interface DownloadFile extends QingzhouModel {
    String ACTION_CODE_FILES = "files";
    String ACTION_CODE_DOWNLOAD = "download";

    String REQUEST_PARAMETER_SERIAL_KEY = "download_serial_key";
    String REQUEST_PARAMETER_FILE_NAMES = "download_file_names";
    String REQUEST_PARAMETER_OFFSET = "download_offset";
    String REQUEST_PARAMETER_BYTES = "download_bytes";

    // 下载文件的父目录或文件本身，安全考虑：子目录会被忽略
    File files(String id) throws Exception;
}