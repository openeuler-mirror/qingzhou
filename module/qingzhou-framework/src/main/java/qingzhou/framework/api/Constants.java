package qingzhou.framework.api;

// 在请求、响应以及API中出现的常量，应定义在这里，框架内部的常量不要放这里
public interface Constants {
    String LOCAL_NODE_NAME = "local";
    String MASTER_APP_NAME = "master";
    String DOWNLOAD_NAME_SEPARATOR = "/";
    String RESPONSE_HEADER_MSG_KEY = "HEADER_MSG_KEY";
    String FILE_FROM_SERVER = "fromServer";
    String FILE_FROM_UPLOAD = "fromUpload";
    String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    String DATA_SEPARATOR = ",";
    String PASSWORD_FLAG = "***************";
    String SINGLE_FIELD_VALIDATE_PARAM = "SINGLE_FIELD_VALIDATE_PARAM";
    String OPTION_GROUP_SEPARATOR = "/";
    String LOGIN_2FA = "password2fa";
}
