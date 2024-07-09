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

    /**
     * 获取下载列表
     * Map<String, List<String[]>> key: 文件组名, value: 文件列表，每个数组元素包含两个元素，第一个元素是文件名，第二个元素是文件大小
     *
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    Map<String, List<String[]>> downloadlist(Request request, Response response) throws Exception;

    /**
     * 获取下载文件
     * File[] 待下载的文件全路径列表
     *
     * @param request
     * @param downloadFileNames 下载文件名列表，元素格式：分组+“/”+文件名
     * @return
     * @throws Exception
     */
    File[] downloadfile(Request request, String[] downloadFileNames) throws Exception;
}

