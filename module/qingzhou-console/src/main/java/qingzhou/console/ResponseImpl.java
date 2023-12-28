package qingzhou.console;

import qingzhou.api.console.data.Datas;
import qingzhou.api.console.data.Response;
import qingzhou.console.util.DownLoadUtil;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ResponseImpl implements Response, Serializable {
    private boolean success = true;
    private String msg;
    private final Datas modelData = new DatasImpl();
    private final Datas monitorData = new DatasImpl();
    private final Datas errorData = new DatasImpl();
    private final Datas attachmentData = new DatasImpl();
    private String contentType;
    private Map<String, Object> downloadData = new HashMap<>();

    @Override
    public Datas modelData() {
        return modelData;
    }

    @Override
    public Datas monitorData() {
        return monitorData;
    }

    @Override
    public Datas errorData() {
        return errorData;
    }

    @Override
    public Datas attachmentData() {
        return attachmentData;
    }

    @Override
    public void setSuccess(boolean success) {
        this.success = success;
    }

    @Override
    public boolean isSuccess() {
        return this.success;
    }

    @Override
    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String getMsg() {
        return this.msg;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public Map<String, Object> downloadData() {
        return downloadData;
    }

    @Override
    public void readDownloadFile(String key, long offset, File[] downloadFiles) throws Exception {
        if (key == null) {
            if (downloadFiles == null || downloadFiles.length == 0) {
                return;
            }
            key = DownLoadUtil.buildDownloadKey(downloadFiles);
        }
        downloadData = DownLoadUtil.downloadFile(key, offset);
    }
}
