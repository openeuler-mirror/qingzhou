package qingzhou.framework.api;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface DownloadModel {
    String ACTION_NAME_DOWNLOADLIST = "downloadlist";
    String ACTION_NAME_DOWNLOADFILE = "downloadfile";
    String PARAMETER_DOWNLOAD_FILE_NAMES = "downloadFileNames";
    String DOWNLOAD_KEY = "DOWNLOAD_KEY";
    String DOWNLOAD_OFFSET = "DOWNLOAD_OFFSET";

    @ModelAction(name = ACTION_NAME_DOWNLOADLIST,
            icon = "download-alt",
            nameI18n = {"下载", "en:Download"},
            infoI18n = {"获取该组件可下载文件的列表。",
                    "en:Gets a list of downloadable files for this component."})
    default void downloadlist(Request request, Response response) throws Exception {
        Map<String, List<String[]>> result = downloadlist0(request, response);
        if (result != null && !result.isEmpty()) {
            HashMap<String, String> map = new HashMap<>();
            if (result.size() == 1) {
                for (String[] file : result.values().iterator().next()) {
                    map.put(file[0], file[0] + " (" + file[1] + ")");
                }
            } else {
                for (Map.Entry<String, List<String[]>> entry : result.entrySet()) {
                    String group = entry.getKey();
                    for (String[] file : entry.getValue()) {
                        map.put(group + Constants.DOWNLOAD_NAME_SEPARATOR + file[0], file[0] + " (" + file[1] + ")");
                    }
                }
            }
            response.addData(map);
        }
    }

    Map<String, List<String[]>> downloadlist0(Request request, Response response) throws Exception;

    @ModelAction(name = ACTION_NAME_DOWNLOADFILE,
            icon = "download-alt",
            nameI18n = {"下载文件", "en:Download File"},
            infoI18n = {"下载指定的文件集合，这些文件须在该组件的可下载文件列表内。注：指定的文件集合须以 " + PARAMETER_DOWNLOAD_FILE_NAMES + " 参数传递给服务器，多个文件之间用英文逗号分隔。",
                    "en:Downloads the specified set of files that are in the component list of downloadable files. Note: The specified file set must be passed to the server with the " + PARAMETER_DOWNLOAD_FILE_NAMES + " parameter, and multiple files are separated by commas."})
    default void downloadfile(Request request, Response response) throws Exception {
        String key = request.getParameter(DOWNLOAD_KEY);
        if (key == null || key.trim().isEmpty()) {
            String downloadFileNames = request.getParameter(PARAMETER_DOWNLOAD_FILE_NAMES);

            // check
            if (downloadFileNames == null || downloadFileNames.trim().isEmpty()) {
                response.setSuccess(false);
                return;
            }
            List<File> downloadFiles = downloadfile0(request, downloadFileNames);
//            response.readDownloadFile(null, 0, downloadFiles.toArray(new File[0])); todo
        } else {
            long offset = Long.parseLong(request.getParameter(DOWNLOAD_OFFSET));
//            response.readDownloadFile(key, offset, null); todo
        }
    }

    List<File> downloadfile0(Request request, String downloadFileNames) throws Exception;
}
