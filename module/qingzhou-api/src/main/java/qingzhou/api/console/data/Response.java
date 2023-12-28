package qingzhou.api.console.data;

import java.io.File;
import java.util.Map;

public interface Response {
    Datas modelData();

    Datas monitorData();

    Datas errorData();

    Datas attachmentData();

    void setSuccess(boolean success);

    boolean isSuccess();

    void setMsg(String msg);

    String getMsg();

    String getContentType();

    Map<String, Object> downloadData();

    void readDownloadFile(String key, long offset, File[] downloadFiles) throws Exception;
}
