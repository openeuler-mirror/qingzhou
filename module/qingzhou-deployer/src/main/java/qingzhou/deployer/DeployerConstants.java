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
    String MODEL_INSTANCE = "instance";
    String MODEL_USER = "user";
    String MODEL_PASSWORD = "password";

    // 系统内部 Action
    String ACTION_CHECK = "check";
    String ACTION_REGISTER = "register";
    String ACTION_MANAGE = "manage";
    String ACTION_INDEX = "index";
    String ACTION_UPLOAD = "upload";
    // 系统内部 Action For Agent
    String ACTION_DOWNLOAD_PAGE = "ACTION_DOWNLOAD_PAGE";
    String ACTION_INSTALL_APP = "ACTION_INSTALL_APP";
    String ACTION_UNINSTALL_APP = "ACTION_UNINSTALL_APP";
    String ACTION_START_APP = "ACTION_START_APP";
    String ACTION_STOP_APP = "ACTION_STOP_APP";
    String ACTION_INSTALL_VERSION = "ACTION_INSTALL_VERSION";
    String ACTION_UNINSTALL_VERSION = "ACTION_UNINSTALL_VERSION";
    String ACTION_CONFIRMKEY = "confirmKey";

    // 内部通信参数

    String UPLOAD_APP_NAME = "UPLOAD_APP_NAME";
    String UPLOAD_FILE_ID = "UPLOAD_FILE_ID";
    String UPLOAD_FILE_NAME = "UPLOAD_FILE_NAME";
    String UPLOAD_FILE_PREFIX_FLAG = "UPLOAD_FILE_PREFIX_FLAG";
    String UPLOAD_FILE_TEMP_SUB_DIR = "UPLOAD_FILE_TEMP_SUB_DIR";

    String DOWNLOAD_SERIAL_KEY = "DOWNLOAD_SERIAL_KEY";
    String DOWNLOAD_OFFSET = "DOWNLOAD_OFFSET";
    String DOWNLOAD_FILE_NAMES = "DOWNLOAD_FILE_NAMES";

    String DOWNLOAD_PAGE_APP = "DOWNLOAD_PAGE_APP";
    String DOWNLOAD_PAGE_DIR = "DOWNLOAD_PAGE_DIR";
    String DOWNLOAD_PAGE_ROOT_DIR = "apps";

    String REST_PREFIX = "/rest";
    String JSON_VIEW_FLAG = "json";
    String CHECK_FINGERPRINT = "fingerprint";
    String DO_REGISTER = "doRegister";
    String PASSWORD_LAST_MODIFIED_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    String FIELD_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    String RANGE_DATE_TIME_FORMAT = "yyyy-MM-DD HH:mm:ss";
    String APP_STARTED = "Started";
    String APP_STOPPED = "Stopped";
    String DEFAULT_DATA_SEPARATOR = ",";
    String DOWNLOAD_FILE_NAMES_SP = "/"; // |/\:*?"<> 这些都是 windows 平台u支持的文件名，linux 上 / 不支持文件名
    String DOWNLOAD_FILE_GROUP_SP = "/";
}
