package qingzhou.api.console.data;

import java.io.File;
import java.util.Map;

public interface Response {
    Datas modelData();// todo 这些或许不应该设计在api里面，因为用户可能不关心？

    Datas monitorData();// todo 这些或许不应该设计在api里面，因为用户可能不关心？

    Datas errorData();// todo 这些或许不应该设计在api里面，因为用户可能不关心？

    Datas attachmentData();// todo 这些或许不应该设计在api里面，因为用户可能不关心？

    void setSuccess(boolean success);

    boolean isSuccess();

    void setMsg(String msg);

    String getMsg();

    Map<String, Object> downloadData();// todo 这些或许不应该设计在api里面，因为用户可能不关心？

    void readDownloadFile(String key, long offset, File[] downloadFiles) throws Exception;// todo 这些或许不应该设计在api里面，因为用户可能不关心？
}
