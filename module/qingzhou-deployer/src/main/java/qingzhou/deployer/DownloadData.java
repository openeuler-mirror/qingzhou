package qingzhou.deployer;

public class DownloadData extends ResponseData {
    public static final String DOWNLOAD_SERIAL_KEY = "DOWNLOAD_SERIAL_KEY";
    public static final String DOWNLOAD_OFFSET = "DOWNLOAD_OFFSET";
    public static final String DOWNLOAD_FILE_NAMES = "DOWNLOAD_FILE_NAMES";
    public static final String DOWNLOAD_FILE_NAMES_SP = "/"; // |/\:*?"<> 这些都是 windows 平台u支持的文件名，linux 上 / 不支持文件名
    public static final String DOWNLOAD_FILE_GROUP_SP = "/";

    public byte[] block;
    public String downloadName;
}
