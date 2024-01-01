package qingzhou.framework.util;

import java.util.HashMap;
import java.util.Map;

public interface Constants { // TODO 需要清理失效的项目
    String QZ_VERSION_NAME = "version";
    String QZ_ = "QZ-";
    String STARTUP_ARGS_SKIP_CHARACTER_CHECK = "%${}";
    String JAVA_HOME_KEY = "JAVA_HOME";
    String DEFAULT_PASSWORD = "jo+fvRppEjYJWnAqSYOpfg==";

    String remoteApp = "/remote";
    String REGISTERED_URI = "/register";
    String NODE_TYPE_REGISTERED = "REGISTERED";

    String remotePath = "/callRemote";
    String uploadPath = "/uploadFile";
    String deleteFilePath = "/deleteFile";
    String ACTION_NAME_INDEX = "index";

    String MANUAL_PDF = "manual";
    String defaultPassword = "jo+fvRppEjYJWnAqSYOpfg==";
    String DEFAULT_DIGEST_ALG = "SHA-256";// 不要修改，否则可能导致无法向下兼容以前生成的 动态密码
    long DAY_MILLIS_VALUE = 24 * 60 * 60 * 1000; // 一天的毫秒值

    String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    String MONITOR_MODEL_SEPARATOR = "-";
    String MONITOR_EXT_SEPARATOR = ":";

    String[] STATIC_RES_SUFFIX = {".html", ".js", ".css", ".ico", ".jpg", ".png", ".gif", ".ttf", ".woff", ".eot", ".svg", ".pdf"};
    Map<String, String> loggerNameDirMap = new HashMap<>();
    String HEADER_NAME_CLI = "REQUEST-FROM-CLI";
    String QINGZHOU_DEFAULT_APP_NAME = "default";

    String AutoRegisterK = "k";
    String AutoRegisterMsg = "msg";
    String AutoRegisterSuccess = "success";

    static void setLoggerNameDir(String loggerName, String dir) {
        loggerNameDirMap.put(loggerName, dir);
    }

    String MODEL_NAME_license = "license";
    String MODEL_NAME_blockedthread = "blockedthread";
    String MODEL_NAME_busythread = "busythread";
    String MODEL_NAME_encryptor = "encryptor";
    String MODEL_NAME_centralizedconfig = "centralizedconfig";
    String MODEL_NAME_node = "node";
    String MODEL_NAME_cluster = "cluster";
    String MODEL_NAME_sessionserver = "sessionserver";
    String MODEL_NAME_loadbalanceserver = "loadbalanceserver";
    String MODEL_NAME_mqserver = "mqserver";
    String MODEL_NAME_host = "host";
    String MODEL_NAME_datasource = "datasource";
    String MODEL_NAME_failoverdatasource = "failoverdatasource";
    String MODEL_NAME_jdbcurltemplate = "jdbcurltemplate";
    String MODEL_NAME_realm = "realm";
    String MODEL_NAME_security = "security";
    String MODEL_NAME_consolesecurity = "consolesecurity";
    String MODEL_NAME_realmuser = "realmuser";
    String MODEL_NAME_server = "server";
    String MODEL_NAME_connector = "connector";
    String MODEL_NAME_app = "app";
    String MODEL_NAME_jndi = "jndi";
    String MODEL_NAME_jvm = "jvm";
    String MODEL_NAME_operatingsystem = "operatingsystem";
    String MODEL_NAME_index = "index";
    String MODEL_NAME_home = "home";
    String MODEL_NAME_jcaconnectionpool = "jcaconnectionpool";
    String MODEL_NAME_jcaadminobject = "jcaadminobject";
    String MODEL_NAME_javamail = "javamail";
    String MODEL_NAME_user = "user";
    String MODEL_NAME_role = "role";
    String MODEL_NAME_userrole = "userrole";
    String MODEL_NAME_serverlog = "serverlog";
    String MODEL_NAME_auditlog = "auditlog";
    String MODEL_NAME_auditconfig = "auditconfig";
    String MODEL_NAME_accesslog = "accesslog";
    String MODEL_NAME_logpush = "logpush";
    String MODEL_NAME_appversion = "appversion";
    String MODEL_NAME_backup = "backup";
    String MODEL_NAME_tenant = "tenant";
    String MODEL_NAME_sshkey = "sshkey";
    String MODEL_NAME_overview = "overview";
    String MODEL_NAME_notice = "notice";
    String MODEL_NAME_password = "password";
}
