package qingzhou.deployer;

public interface DeployerConstants {
    String APP_SYSTEM = "-";
    String ACTION_INVOKE_CHARSET = "UTF-8";

    // master model
    String MODEL_MASTER = "master";
    String MODEL_AGENT = "agent";
    String MODEL_INDEX = "index";
    String MODEL_APP = "app";
    String MODEL_USER = "user";
    String MODEL_PASSWORD = "password";

    // 系统内部 action
    String ACTION_CHECK = "check";
    String ACTION_REGISTER = "register";
    String ACTION_INSTALL = "install";
    String ACTION_UNINSTALL = "uninstall";
    String ACTION_MANAGE = "manage";
    String ACTION_INDEX = "index";
    String ACTION_UPLOAD = "upload";
    String ACTION_REFRESHKEY = "refreshKey";

    // 其它参数
    String INSTANCE_LOCAL = "local";
    String PASSWORD_LAST_MODIFIED_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    String DEFAULT_USER_QINGZHOU = "qingzhou";
    String DEFAULT_DATA_SEPARATOR = ",";
    String INSTALLER_PARAMETER_FILE_ID = "fileId";
    String INSTALLER_PARAMETER_FILE_NAME = "fileName";
    String INSTALLER_PARAMETER_FILE_BYTES = "fileBytes";
    String DOWNLOAD_KEY = "DOWNLOAD_KEY";
    String DOWNLOAD_OFFSET = "DOWNLOAD_OFFSET";
    String DOWNLOAD_BLOCK = "DOWNLOAD_BLOCK";
    String DOWNLOAD_DOWNLOAD_FILE_NAMES = "downloadFileNames";
    String REST_PREFIX = "/rest";
    String jsonView = "json";
    String CHECK_FINGERPRINT = "fingerprint";
    String BATCH_ID_SEPARATOR = ",";
}
