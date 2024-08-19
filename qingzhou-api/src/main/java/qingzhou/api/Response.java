package qingzhou.api;

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

    /**
     * 设置操作返回的消息。
     */
    void setMsg(String msg);

    /**
     * 添加数据到响应中，数据以Map形式组织。
     */
    void addData(Map<String, String> data);

    /**
     * 以对象形式添加数据到响应。
     */
    void addModelData(ModelBase data) throws Exception;

    /**
     * 设置响应类型
     */
    void setContentType(String contentType);
}
