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

    // 自定义返回数据，会忽略 success 和 msg 数据
    void setData(Serializable data);

    // 设置响应类型
    void setContentType(String contentType);

    void setHeader(String name, String value);

    void setStatusCode(int sc);
}
