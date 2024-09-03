package qingzhou.deployer;

public interface DeployerConstants {
    // master model
    String APP_MASTER = "master";
    String MODEL_INDEX = "index";
    String MODEL_APP = "app";
    String MODEL_INSTANCE = "instance";
    String MODEL_PASSWORD = "password";

    // instance model
    String APP_INSTANCE = "instance";
    String MODEL_HOME = "home";
    String MODEL_INSTALLER = "installer";

    // api 级 action
    String ACTION_CREATE = "create";
    String ACTION_ADD = "add";
    String ACTION_EDIT = "edit";
    String ACTION_UPDATE = "update";
    String ACTION_LIST = "list";
    String ACTION_DELETE = "delete";
    String ACTION_SHOW = "show";
    String ACTION_MONITOR = "monitor";
    String ACTION_FILES = "files";
    String ACTION_DOWNLOAD = "download";

    // 系统内部 action
    String ACTION_MANAGE = "manage";
    String ACTION_REGISTER = "register";
    String ACTION_CHECKREGISTRY = "checkRegistry";
    String ACTION_INDEX = "index";
    String ACTION_INSTALL = "install";
    String ACTION_UNINSTALL = "uninstall";
    String ACTION_UPLOAD = "upload";

    // 管理类型
    String MANAGE_APP = "app";
    String MANAGE_INSTANCE = "instance";

    // 其它参数
    String INSTANCE_LOCAL = "local";
    String PASSWORD_LAST_MODIFIED_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    String DEFAULT_DATA_SEPARATOR = ",";
    String APP_KEY_ID = "id";
    String APP_KEY_PATH = "path";
}
