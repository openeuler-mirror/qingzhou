package qingzhou.framework.console;

public interface ConsoleConstants { // TODO 需要清理失效的项目
    String localKeyName = "localKey";
    String publicKeyName = "publicKey";
    String privateKeyName = "privateKey";
    String remotePublicKeyName = "remotePublicKey";

    String QZ_ = "QZ-";
    String remoteApp = "/remote";
    String remotePath = "/callRemote";
    String uploadPath = "/uploadFile";
    String deleteFilePath = "/deleteFile";
    String ACTION_NAME_INDEX = "index";

    String GROUP_SEPARATOR = "/";
    String LOGIN_2FA = "password2fa";
    String OPTION_GROUP_SEPARATOR = "/";
    String SINGLE_FIELD_VALIDATE_PARAM = "SINGLE_FIELD_VALIDATE_PARAM";
    String DOWNLOAD_NAME_SEPARATOR = "/";

    String MANUAL_PDF = "manual";
    long DAY_MILLIS_VALUE = 24 * 60 * 60 * 1000; // 一天的毫秒值
    String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    String MODEL_NAME_node = "node";
    String MODEL_NAME_cluster = "cluster";
    String MODEL_NAME_index = "index";
    String MODEL_NAME_home = "home";
    String MODEL_NAME_user = "user";
    String MODEL_NAME_role = "role";
    String MODEL_NAME_userrole = "userrole";
    String MODEL_NAME_auditlog = "auditlog";
    String MODEL_NAME_auditconfig = "auditconfig";
    String MODEL_NAME_app = "app";
    String MODEL_NAME_backup = "backup";
    String MODEL_NAME_password = "password";
    String DATA_SEPARATOR = ",";
    String LOCAL_NODE_NAME = "local";
    String MASTER_APP_NAME = "master";
    String RESPONSE_HEADER_MSG_KEY = "HEADER_MSG_KEY";
}
