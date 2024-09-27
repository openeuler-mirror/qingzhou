package qingzhou.deployer;

public interface DeployerConstants {
    // 系统环境
    String APP_SYSTEM = "-";
    String INSTANCE_LOCAL = "local";
    String ACTION_INVOKE_CHARSET = "UTF-8";
    String DEFAULT_USER_QINGZHOU = "qingzhou";

    // Master 模块
    String MODEL_MASTER = "master";
    String MODEL_AGENT = "agent";
    String MODEL_INDEX = "index";
    String MODEL_APP = "app";
    String MODEL_USER = "user";
    String MODEL_PASSWORD = "password";

    // 系统内部 Action
    String ACTION_CHECK = "check";
    String ACTION_REGISTER = "register";
    String ACTION_MANAGE = "manage";
    String ACTION_INDEX = "index";
    String ACTION_UPLOAD = "upload";
    String ACTION_REFRESHKEY = "refreshKey";

    // 内部通信参数
    String UPLOAD_FILE_ID = "QINGZHOU_UPLOAD_FILE_ID";
    String UPLOAD_FILE_NAME = "QINGZHOU_UPLOAD_FILE_NAME";
    String UPLOAD_FILE_BYTES = "QINGZHOU_UPLOAD_FILE_BYTES";
    String UPLOAD_FILE_PREFIX_FLAG = "QINGZHOU_UPLOAD_FILE_PREFIX_FLAG";
    String UPLOAD_FILE_TEMP_SUB_DIR = "QINGZHOU_UPLOAD_FILE_TEMP_SUB_DIR";
    String DOWNLOAD_KEY = "QINGZHOU_DOWNLOAD_KEY";
    String DOWNLOAD_OFFSET = "QINGZHOU_DOWNLOAD_OFFSET";
    String DOWNLOAD_BLOCK = "QINGZHOU_DOWNLOAD_BLOCK";
    String DOWNLOAD_FILE_NAMES = "QINGZHOU_DOWNLOAD_FILE_NAMES";

    // 控制台参数
    String REST_PREFIX = "/rest";
    String JSON_VIEW = "json";

    // 模块间通信参数
    String JSON_DATA = "data";
    String CHECK_FINGERPRINT = "fingerprint";
    String PASSWORD_LAST_MODIFIED_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    String FIELD_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    String BATCH_ID_SEPARATOR = ",";
    String DEFAULT_DATA_SEPARATOR = ",";

    // 其它
    int DOWNLOAD_BLOCK_SIZE = Integer.parseInt(System.getProperty("qingzhou.DOWNLOAD_BLOCK_SIZE", String.valueOf(1024 * 1024 * 2)));
}
