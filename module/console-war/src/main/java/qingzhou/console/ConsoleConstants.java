package qingzhou.console;

public interface ConsoleConstants {
    String LOGIN_2FA = "password2fa";
    String OPTION_GROUP_SEPARATOR = "/";
    long DAY_MILLIS_VALUE = 24 * 60 * 60 * 1000; // 一天的毫秒值
    String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    String MODEL_NAME_node = "node";// todo: 如何保持和 mode 的同步？
    String MODEL_NAME_password = "password";// todo: 如何保持和 mode 的同步？
    String DATA_SEPARATOR = ",";
    String RESPONSE_HEADER_MSG_KEY = "HEADER_MSG_KEY";
    String GROUP_SEPARATOR = "/";
    String REGISTER_URI = "/register";

    String MANAGE_TYPE_APP = "app";
    String MANAGE_TYPE_NODE = "node";

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
