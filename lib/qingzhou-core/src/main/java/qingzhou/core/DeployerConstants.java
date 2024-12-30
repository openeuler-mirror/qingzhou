package qingzhou.core;

import java.util.*;

import qingzhou.api.ActionType;
import qingzhou.api.AppContext;
import qingzhou.api.Lang;

public interface DeployerConstants {
    // 系统环境
    String APP_SYSTEM = "-";
    String INSTANCE_LOCAL = "localhost";
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

    Set<String> NONE_ROLE_SYSTEM_MODELS = Collections.unmodifiableSet(new HashSet<String>() {{
        add(DeployerConstants.MODEL_PASSWORD);
        add(DeployerConstants.MODEL_INDEX);
    }});
    Map<String, Set<String>> NONE_ROLE_SYSTEM_MODEL_ACTIONS = Collections.unmodifiableMap(new HashMap<String, Set<String>>() {{
        put(DeployerConstants.MODEL_APP, Collections.unmodifiableSet(new HashSet<String>() {{
            add(DeployerConstants.ACTION_MANAGE);
        }}));
    }});
    Set<String> NONE_ROLE_NONE_SYSTEM_MODELS = Collections.unmodifiableSet(new HashSet<String>() {{
        add(AppContext.APP_HOME_MODEL);
    }});

    // 系统内部 Action
    String ACTION_CHECK = "check";
    String ACTION_REGISTER = "register";
    String ACTION_MANAGE = "manage";
    String ACTION_START = "start";
    String ACTION_STOP = "stop";
    String ACTION_INDEX = "index";
    String ACTION_UPLOAD = "upload";
    // 系统内部 Action For Agent
    String ACTION_INSTALL_APP = "ACTION_INSTALL_APP";
    String ACTION_UNINSTALL_APP = "ACTION_UNINSTALL_APP";
    String ACTION_START_APP = "ACTION_START_APP";
    String ACTION_STOP_APP = "ACTION_STOP_APP";
    String ACTION_UPDATE_APP = "ACTION_UPDATE_APP";
    String ACTION_INSTALL_VERSION = "ACTION_INSTALL_VERSION";
    String ACTION_UNINSTALL_VERSION = "ACTION_UNINSTALL_VERSION";
    String ACTION_CONFIRMKEY = "confirmKey";
    String ACTION_GC = "gc";

    // 内部通信参数

    String UPLOAD_APP_NAME = "UPLOAD_APP_NAME";
    String UPLOAD_FILE_ID = "UPLOAD_FILE_ID";
    String UPLOAD_FILE_NAME = "UPLOAD_FILE_NAME";
    String UPLOAD_FILE_PREFIX_FLAG = "UPLOAD_FILE_PREFIX_FLAG";
    String UPLOAD_FILE_TEMP_SUB_DIR = "UPLOAD_FILE_TEMP_SUB_DIR";

    String QINGZHOU_MANAGER_USER_TYP = "Qingzhou_Manager";
    String USER_ROLE_SP = ";";
    String ROLE_URI_SP = ";";
    String REST_PREFIX = "/rest";
    String JSON_VIEW_FLAG = "json";
    String CHECK_FINGERPRINT = "fingerprint";
    String DO_REGISTER = "doRegister";
    String PASSWORD_LAST_MODIFIED_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    String APP_STARTED = "Started"; // 保持一致：qingzhou.core.registry.AppState.Started
    String APP_STOPPED = "Stopped"; // 保持一致：qingzhou.core.registry.AppState.Stopped
    String DEPLOYMENT_PROPERTIES = "deploymentProperties"; // 保持一致：qingzhou.app.system.business.App.deploymentProperties
    String MULTISELECT_GROUP_SEPARATOR = ":";
    String SUB_MENU_PARAMETER_FLAG = ActionType.sub_menu.name() + "_parameter_flag_";
    String RETURNS_LINK_PARAM_NAME_RETURNSID = "PARAM_NAME_RETURNSID";

    String QINGZHOU_PROPERTIES_FILE = "qingzhou.properties";
    String QINGZHOU_PROPERTIES_APP_MAIN_CLASS = "qingzhou.app.main.class";
    //通过contains匹配，忽略大小写，支持逗号隔开
    String QINGZHOU_PROPERTIES_APP_SCAN_FILENAME = "qingzhou.app.scan.filename";
    String QINGZHOU_PROPERTIES_APP_SCAN_INCLUDE = "qingzhou.app.scan.include";
    String QINGZHOU_PROPERTIES_APP_SCAN_EXCLUDE = "qingzhou.app.scan.exclude";
    String LOGIN_OTP = "otp";
    String VALIDATION_ADD_FLAG = "VALIDATION_ADD_FLAG";
    String DYNAMIC_OPTION_FIELD = "DYNAMIC_OPTION_FIELD";
    String LIST_ALL_FIELDS = "LIST_ALL_FIELDS";

    Lang DEFAULT_LANG = Lang.zh;
    ThreadLocal<Lang> SESSION_LANG = ThreadLocal.withInitial(() -> DEFAULT_LANG); // 支持在 qingzhou.api.AuthAdapter.doAuth 中使用国际化
}
