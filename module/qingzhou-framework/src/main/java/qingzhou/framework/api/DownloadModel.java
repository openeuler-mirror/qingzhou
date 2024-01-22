package qingzhou.framework.api;

import qingzhou.framework.util.FileUtil;
import qingzhou.framework.util.HexUtil;
import qingzhou.framework.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface DownloadModel {
    int DOWNLOAD_BLOCK_SIZE = Integer.parseInt(System.getProperty("qingzhou.DOWNLOAD_BLOCK_SIZE", String.valueOf(1024 * 1024 * 2)));

    String ACTION_NAME_DOWNLOADLIST = "downloadlist";
    String ACTION_NAME_DOWNLOADFILE = "downloadfile";

    // 交互参数
    String PARAMETER_DOWNLOAD_FILE_NAMES = "downloadFileNames";
    String DOWNLOAD_FILE_NAME_SEPARATOR = ",";
    String DOWNLOAD_NAME_SEPARATOR = "/";

    // 过程入参
    String DOWNLOAD_KEY = "DOWNLOAD_KEY";
    String DOWNLOAD_OFFSET = "DOWNLOAD_OFFSET";

    // 过程出参
    String DOWNLOAD_BLOCK = "DOWNLOAD_BLOCK";

    @ModelAction(name = ACTION_NAME_DOWNLOADLIST,
            showToList = true,
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
                        map.put(group + DOWNLOAD_NAME_SEPARATOR + file[0], file[0] + " (" + file[1] + ")");
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
        File keyDir = getAppContext().getCache(); // todo: 需要将这个返回值固定下来，否则续传时候会找不到文件

        String downloadKey = request.getParameter(DOWNLOAD_KEY);
        if (downloadKey == null || downloadKey.trim().isEmpty()) {
            String downloadFileNames = request.getParameter(PARAMETER_DOWNLOAD_FILE_NAMES);

            // check
            if (downloadFileNames == null || downloadFileNames.trim().isEmpty()) {
                response.setMsg("No file name found.");
                response.setSuccess(false);
                return;
            }

            File[] downloadFiles = downloadfile0(request, downloadFileNames.split(DOWNLOAD_FILE_NAME_SEPARATOR));
            downloadKey = buildDownloadKey(downloadFiles, keyDir);
        }

        long downloadOffset = 0;
        String downloadOffsetParameter = request.getParameter(DOWNLOAD_OFFSET);
        if (downloadOffsetParameter != null && !downloadOffsetParameter.trim().isEmpty()) {
            downloadOffset = Long.parseLong(downloadOffsetParameter.trim());
        }

        Map<String, String> downloadedStatus = downloadFile(downloadKey, downloadOffset, keyDir);
        if (downloadedStatus != null) {
            response.addData(downloadedStatus);
        }
    }

    // 计算得出需要下载的实际文件列表
    File[] downloadfile0(Request request, String[] downloadFileNames) throws Exception;

    AppContext getAppContext(); // 已在 ModelBase 里自动实现

    // 为支持大文件续传，下载必需有 key
    static Map<String, String> downloadFile(String key, long offset, File keyDir) throws Exception {
        if (StringUtil.isBlank(key) || !FileUtil.newFile(keyDir, key).exists()) {
            return null;
        }
        if (offset < 0) {
            return null;
        }
        Map<String, String> result = new HashMap<>();

        File downloadFile = FileUtil.newFile(keyDir, key);
        result.put(DOWNLOAD_KEY, key);

        byte[] byteRead = null;
        try (RandomAccessFile raf = new RandomAccessFile(downloadFile, "r")) {
            raf.seek(offset);
            byte[] block = new byte[DOWNLOAD_BLOCK_SIZE];
            int read = raf.read(block);
            if (read > 0) { // ==0 表示上次正好读取到结尾
                if (read == block.length) {
                    byteRead = block;
                } else {
                    byteRead = new byte[read];
                    System.arraycopy(block, 0, byteRead, 0, read);
                }
                result.put(DOWNLOAD_BLOCK, HexUtil.bytesToHex(byteRead));
                offset = raf.getFilePointer();
            }
        }

        if (byteRead != null && byteRead.length == DOWNLOAD_BLOCK_SIZE) {
            result.put(DOWNLOAD_OFFSET, String.valueOf(offset));
        } else {
            result.put(DOWNLOAD_OFFSET, String.valueOf(-1L));
            File temp = FileUtil.newFile(keyDir, key);
            FileUtil.forceDeleteQuietly(temp);
        }

        return result;
    }

    // 为支持大文件续传，下载必需有 key
    static String buildDownloadKey(File[] downloadFiles, File keyDir) throws IOException {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        String keySP = "-";

        // 清理历史的临时文件
        String[] list = keyDir.list();
        if (list != null) {
            for (String historicalKey : list) {
                boolean delete = true;
                try {
                    int i = historicalKey.indexOf(keySP);
                    long time = format.parse(historicalKey.substring(0, i)).getTime();
                    if (System.currentTimeMillis() - time < 1000 * 60 * 60 * 24) { // 保留下载失败的文件 key 最多一天，之后清理
                        delete = false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (delete) {
                    File temp = FileUtil.newFile(keyDir, historicalKey);
                    FileUtil.forceDeleteQuietly(temp);
                }
            }
        }

        String key = format.format(new Date()) + keySP + UUID.randomUUID().toString().replace("-", "");
        File zipTo = FileUtil.newFile(keyDir, key);
        File tempDir = FileUtil.newFile(keyDir, UUID.randomUUID().toString());// 保障压缩文件的层次结构
        for (File file : downloadFiles) {
            if (file.exists()) {
                File copyTo = new File(tempDir, file.getName());
                if (file.isDirectory()) {
                    FileUtil.mkdirs(copyTo);
                }
                FileUtil.copyFileOrDirectory(file, copyTo);
            }
        }
        FileUtil.zipFiles(tempDir, zipTo, false);
        FileUtil.forceDeleteQuietly(tempDir);
        return key;
    }
}
