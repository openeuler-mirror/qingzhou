package qingzhou.console.util;

import java.util.HashMap;
import java.util.Map;

public interface Constants { // TODO 需要清理失效的项目
    String QZ_VERSION_NAME = "version";
    String QZ_ = "QZ-";
    String BLACK_LIST = "org.codehaus.groovy.runtime.,org.apache.commons.collections.functors.,org.apache.commons.collections4.functors.,org.apache.xalan,java.lang.Process,javax.management.BadAttributeValueExpException,com.sun.org.apache.xalan,org.springframework.beans.factory.ObjectFactory,org.apache.commons.fileupload,org.apache.commons.beanutils";// 安全起见，不要修改，应该追加
    String WHITE_LIST = "";// 安全起见，默认没有白名单，不允许序列化任何类
    String UMASK_KEY = "qingzhou_UMASK";
    String STARTUP_ARGS_SKIP_CHARACTER_CHECK = "%${}";
    String TRUST_FILE = "trusted-files.txt";
    String JAVA_HOME_KEY = "JAVA_HOME";
    String DEFAULT_PASSWORD = "jo+fvRppEjYJWnAqSYOpfg==";

    // TODO begin: 梳理到对应的位置，都堆在这里太难维护。。。。
    //  不要设置为 final 常量，避免被优化编译到外部jar的类，导致这里修改了，其它jar的类里没有跟着变

    String[] JAVA_9_PLUS = new String[]{"--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.io=ALL-UNNAMED",
            "--add-opens=java.base/java.math=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED",
            "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
            "--add-opens=java.base/java.net=ALL-UNNAMED",
            "--add-opens=java.base/java.text=ALL-UNNAMED",
            "--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED", // for OpenRasp的agent
            "--add-opens=java.base/sun.security.action=ALL-UNNAMED", // for #ITAIT-5445
            "--add-opens=java.sql/java.sql=ALL-UNNAMED",
            "--add-opens=java.rmi/sun.rmi.transport=ALL-UNNAMED",
            "--add-opens=java.management.rmi/javax.management.remote.rmi=ALL-UNNAMED",
            "--add-exports=java.management/com.sun.jmx.remote.security=ALL-UNNAMED"};

    String remoteApp = "/remote";
    String REGISTERED_URI = "/register";
    String NODE_TYPE_REGISTERED = "REGISTERED";


    String remotePath = "/callRemote";
    String uploadPath = "/uploadFile";
    String deleteFilePath = "/deleteFile";
    String ACTION_NAME_INDEX = "index";

    String MANUAL_PDF = "manual";
    String defaultPassword = "jo+fvRppEjYJWnAqSYOpfg==";
    String PASSWORD_FLAG = "***************";
    String FILE_FROM_UPLOAD = "fromUpload";
    String FILE_FROM_SERVER = "fromServer";

    String NOTICE_TYPE_RESTART = "NOTICE_TYPE_RESTART";
    String DEFAULT_DIGEST_ALG = "SHA-256";// 不要修改，否则可能导致无法向下兼容以前生成的 动态密码
    long DAY_MILLIS_VALUE = 24 * 60 * 60 * 1000; // 一天的毫秒值

    String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    String DATA_SEPARATOR = ",";
    String MONITOR_MODEL_SEPARATOR = "-";
    String MONITOR_EXT_SEPARATOR = ":";
    String GROUP_SEPARATOR = "/";
    String[] STATIC_RES_SUFFIX = {".html", ".js", ".css", ".ico", ".jpg", ".png", ".gif", ".ttf", ".woff", ".eot", ".svg", ".pdf"};
    Map<String, String> loggerNameDirMap = new HashMap<>();
    String HEADER_NAME_CLI = "REQUEST-FROM-CLI";
    String SINGLE_FIELD_VALIDATE_PARAM = "SINGLE_FIELD_VALIDATE_PARAM";
    String QINGZHOU_MASTER_APP_NAME = qingzhou.api.Constants.QINGZHOU_MASTER_APP_NAME;
    String QINGZHOU_DEFAULT_APP_NAME = "default";
    String QINGZHOU_DEFAULT_NODE_NAME = "default";
    String LOGIN_2FA = "password2fa";
    String AutoRegisterK = "k";
    String AutoRegisterMsg = "msg";
    String AutoRegisterSuccess = "success";

    static void setLoggerNameDir(String loggerName, String dir) {
        loggerNameDirMap.put(loggerName, dir);
    }

    String RESPONSE_HEADER_MSG_KEY = "HEADER_MSG_KEY";

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
    String MODEL_NAME_favorites = "favorites";
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
    String MODEL_NAME_startupargs = "startupargs";
    String MODEL_NAME_lib = "lib";
    String MODEL_NAME_snapshottemplate = "snapshottemplate";
    String MODEL_NAME_snapshotfile = "snapshotfile";
    String MODEL_NAME_thresholdstrategy = "thresholdstrategy";
    String MODEL_NAME_combinedmonitor = "combinedmonitor";
    String MODEL_NAME_daemonmonitor = "daemonmonitor";
    String MODEL_NAME_upgrade = "upgrade";
    String MODEL_NAME_version = "version";
    String MODEL_NAME_jmx = "jmx";
    String MODEL_NAME_support = "support";
    String MODEL_NAME_classloaded = "classloaded";
    String MODEL_NAME_jvmconfig = "jvmconfig";
    String MODEL_NAME_feature = "feature";
    String MODEL_NAME_cttransformer = "cttransformer";
    String MODEL_NAME_cache = "cache";
    String MODEL_NAME_convert = "convert";
    String MODEL_NAME_registry = "registry";


    // TODO end: 不要设置为 final 常量，避免被优化编译到外部jar的类，导致这里修改了，其它jar的类里没有跟着变
}
