package qingzhou.api.type;

import qingzhou.api.Request;
import qingzhou.api.Response;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Downloadable 接口定义了与下载相关的操作。
 */
public interface Downloadable {
    // 可下载文件操作的常量名称
    String ACTION_NAME_FILES = "files";

    // 下载操作的常量名称
    String ACTION_NAME_DOWNLOAD = "download";

    String PARAMETER_DOWNLOAD_FILE_NAMES = "downloadFileNames";

    String DOWNLOAD_KEY = "DOWNLOAD_KEY";

    String DOWNLOAD_BLOCK = "DOWNLOAD_BLOCK";

    String DOWNLOAD_OFFSET = "DOWNLOAD_OFFSET";

    String DOWNLOAD_FILE_NAME_SEPARATOR = ",";
    String DOWNLOAD_FILE_GROUP_NAME_SEPARATOR = "/";

    Map<String, List<String[]>> downloadlist(Request request, Response response) throws Exception;

    // 计算得出需要下载的实际文件列表
    File[] downloadfile(Request request, String[] downloadFileNames) throws Exception;
}

