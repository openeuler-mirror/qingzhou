package qingzhou.api.type;

public interface Downloadable {
    String ACTION_NAME_FILES = "files";
    String ACTION_NAME_DOWNLOAD = "download";

    int DOWNLOAD_BLOCK_SIZE = Integer.parseInt(System.getProperty("qingzhou.DOWNLOAD_BLOCK_SIZE", String.valueOf(1024 * 1024 * 2)));
    // 交互参数
    String PARAMETER_DOWNLOAD_FILE_NAMES = "downloadFileNames";
    String DOWNLOAD_FILE_NAME_SEPARATOR = ",";
    String DOWNLOAD_NAME_SEPARATOR = "/";
    // 过程入参
    String DOWNLOAD_KEY = "DOWNLOAD_KEY";
    String DOWNLOAD_OFFSET = "DOWNLOAD_OFFSET";
    // 过程出参
    String DOWNLOAD_BLOCK = "DOWNLOAD_BLOCK";
}
