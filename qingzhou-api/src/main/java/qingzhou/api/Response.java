package qingzhou.api;

import java.io.Serializable;

/**
 * 响应接口定义了操作响应结果的方法。
 * 主要用于设置响应的成功状态、消息、总数、分页信息以及数据添加。
 */
public interface Response {
    /**
     * 设置操作成功状态。
     */
    void setSuccess(boolean success);

    /**
     * 设置操作返回的消息。
     */
    void setMsg(String msg);

    void setMsgLevel(MsgLevel msgLevel);

    // 设置响应类型
    void setContentType(String contentType);

    void setHeader(String name, String value);

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

    void setCustomizedDataObject(Serializable customizedDataObject);

    void addDataMap(String key, String value);
}
