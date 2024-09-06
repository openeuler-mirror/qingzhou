package qingzhou.deployer;

public interface DeployerConstants {
    String APP_SYSTEM = "-";
    String ACTION_INVOKE_CHARSET = "UTF-8";

    // master model
    String MODEL_INDEX = "index";
    String MODEL_HOME = "home";
    String MODEL_APP = "app";
    String MODEL_INSTALLER = "installer";
    String MODEL_INSTANCE = "instance";
    String MODEL_PASSWORD = "password";

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

    // 其它参数
    String INSTANCE_LOCAL = "local";
    String PASSWORD_LAST_MODIFIED_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    String DEFAULT_DATA_SEPARATOR = ",";
    String INSTALLER_PARAMETER_FILE_ID = "fileId";
    String INSTALLER_PARAMETER_FILE_NAME = "fileName";
    String INSTALLER_PARAMETER_FILE_BYTES = "fileBytes";
    String DOWNLOAD_KEY="DOWNLOAD_KEY";
    String DOWNLOAD_OFFSET="DOWNLOAD_OFFSET";
    String DOWNLOAD_BLOCK="DOWNLOAD_BLOCK";
    String REST_PREFIX = "/rest";
    String jsonView = "json";
}
