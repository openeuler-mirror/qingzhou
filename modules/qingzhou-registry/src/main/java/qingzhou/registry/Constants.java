package qingzhou.registry;

public interface Constants {
    String LOCAL_INSTANCE_ID = "-";
    String SYSTEM_MODEL_CODE = "-";

    String AI_REQUEST_PARAMETER_PAGE_NUM = "pageNum";
    String WEB_REQUEST_PARAMETER_PAGE_NUM = "pageNum";
    String WEB_RESPONSE_PARAMETER_PAGE_NUM = "pageNum";
    String WEB_RESPONSE_PARAMETER_PAGE_SIZE = "pageSize";
    String WEB_RESPONSE_PARAMETER_TOTAL_SIZE = "totalSize";

    String REMOTE_REGISTER_URI = "/register";
    String REMOTE_INVOKE_URI = "/invoke";
    String REMOTE_UPLOAD_URI = "/upload";
    String REMOTE_PARAMETER_DOMAIN_ID = "domainId";
    String REMOTE_PARAMETER_INSTANCE_ID = "instanceId";
    String REMOTE_PARAMETER_FILE_KEY = "fileKey";
    String REMOTE_PARAMETER_FILE_OFFSET = "offset";
    String REMOTE_UPLOAD_TEMP_KEY_FILE_SP = "=";
    String REMOTE_UPLOAD_MULTIPLE_FILE_SP = ",";
}
