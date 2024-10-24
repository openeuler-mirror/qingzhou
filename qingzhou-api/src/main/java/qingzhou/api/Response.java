package qingzhou.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 响应接口定义了操作响应结果的方法。
 * 主要用于设置响应的成功状态、消息、总数、分页信息以及数据添加。
 */
public interface Response {
    /**
     * 设置操作成功状态。
     */
    void setSuccess(boolean success);

    boolean isSuccess();

    /**
     * 设置操作返回的消息。
     */
    void setMsg(String msg);

    String getMsg();

    void setMsgType(MsgLevel msgLevel);

    MsgLevel getMsgType();

    /**
     * 添加数据到响应中，数据以Map形式组织。
     */
    void addData(Map<String, String> data);

    /**
     * 以对象形式添加数据到响应。
     */
    void addModelData(ModelBase data) throws Exception;

    /**
     * 获得数据列表引用，以便进行清理等操作
     */
    List<Map<String, String>> getDataList();

    int getTotalSize();

    int getPageSize();

    int getPageNum();

    /**
     * 设置响应类型
     */
    void setContentType(String contentType);

    String getContentType();

    void setHeader(String name, String value);

    String getHeader(String name);

    Collection<String> getHeaderNames();

    /**
     * Sets a response header with the given name and date-value. The date is
     * specified in terms of milliseconds since the epoch. If the header had
     * already been set, the new value overwrites the previous one. The
     * <code>containsHeader</code> method can be used to test for the presence
     * of a header before setting its value.
     *
     * @param name the name of the header to set
     * @param date the assigned date value
     */
    void setDateHeader(String name, long date);

    Long getDateHeader(String name);

    Collection<String> getDateHeaderNames();

    void setDownloadName(String downloadName);
}
