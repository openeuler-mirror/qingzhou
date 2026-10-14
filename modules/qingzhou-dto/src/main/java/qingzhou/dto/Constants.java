package qingzhou.dto;

public interface Constants {
    String LOCAL_INSTANCE_ID = "-";
    String SYSTEM_MODEL_CODE = "-";
    String REQUEST_PARAMETER_NAME_LANG = "lang";
    String REQUEST_PARAMETER_NAME_PAGE_NUM = "pageNum";
    String REQUEST_PARAMETER_NAME_PAGE_SIZE = "pageSize";
    String REQUEST_PARAMETER_NAME_TOTAL_SIZE = "totalSize";

    String AGENT_INVOKE_URI = "/invoke";
    String AGENT_UPLOAD_URI = "/upload";
    String AGENT_UPLOAD_FILE_KEY = "fileKey";
    String AGENT_UPLOAD_MULTIPLE_FILE_FIELD_SP = ",";
    String AGENT_UPLOAD_MULTIPLE_FILE_NAME_SP = "=";
    String AGENT_REGISTER_TOKEN_SP = "\n";
    /**
     * 密钥轮换宽限：Refresh 轮换时记录旧钥，AppStubRemoteImpl 宽限期内继续用旧钥加密
     * 宽限期：须覆盖轮换 RTT 并大于单次远程调用的最大时长（含分片上传），否则超长调用仍会失败
     */
    long AGENT_KEY_GRACE_MILLIS = 60_000;
}
