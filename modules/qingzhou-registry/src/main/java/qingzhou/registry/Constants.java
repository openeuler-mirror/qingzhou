package qingzhou.registry;

public interface Constants {
    String LOCAL_INSTANCE_ID = "-";
    String SYSTEM_MODEL_CODE = "-";

    // 平台内部动作码
    String ACTION_CODE_ADD = "add";
    String ACTION_CODE_UPDATE = "update";
    String ACTION_CODE_SHOW = "show";
    String ACTION_CODE_DELETE = "delete";
    String ACTION_CODE_PAGE = "page";
    String ACTION_CODE_CURRENT = "current";
    String ACTION_CODE_DOWNLOAD = "download";
    String ACTION_CODE_MONITOR = "monitor";
    String ACTION_CODE_FILE = "file";
    String ACTION_CODE_NAMESPACE = "namespace";

    String AI_REQUEST_PARAMETER_PAGE_NUM = "pageNum";
    String WEB_REQUEST_PARAMETER_PAGE_NUM = "pageNum";
    String WEB_RESPONSE_PARAMETER_PAGE_NUM = "pageNum";
    String WEB_RESPONSE_PARAMETER_PAGE_SIZE = "pageSize";
    String WEB_RESPONSE_PARAMETER_TOTAL_SIZE = "totalSize";

    String REMOTE_REGISTER_URI = "/register";
    String REMOTE_INVOKE_URI = "/invoke";
    String REMOTE_UPLOAD_URI = "/upload";
    String REMOTE_PARAMETER_DOMAIN_ID = "d";
    String REMOTE_PARAMETER_INSTANCE_ID = "i";
    String REMOTE_PARAMETER_FILE_KEY = "f";
    String REMOTE_PARAMETER_FILE_OFFSET = "o";
    String REMOTE_UPLOAD_TEMP_KEY_FILE_SP = "=";
    String REMOTE_UPLOAD_MULTIPLE_FILE_SP = ",";
}
