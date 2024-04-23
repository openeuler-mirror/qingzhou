package qingzhou.api;

import java.util.Map;

/**
 * 响应接口定义了操作响应结果的方法。
 * 主要用于设置响应的成功状态、消息、总数、分页信息以及数据添加。
 */
public interface Response {
    /**
     * 设置操作成功状态。
     *
     * @param success 指示操作是否成功。
     */
    void setSuccess(boolean success);

    /**
     * 设置操作返回的消息。
     *
     * @param msg 操作返回的消息文本。
     */
    void setMsg(String msg);

    /**
     * 设置总数，通常用于分页展示数据时的总记录数。
     *
     * @param totalSize 数据总条数。
     */
    void setTotalSize(int totalSize);

    /**
     * 设置每页显示的记录数。
     *
     * @param pageSize 每页的大小。
     */
    void setPageSize(int pageSize);

    int getPageSize();

    /**
     * 设置当前页码。
     *
     * @param pageNum 当前页的编号。
     */
    void setPageNum(int pageNum);

    /**
     * 添加数据到响应中，数据以Map形式组织。
     *
     * @param data 要添加的数据，键值对形式。
     */
    void addData(Map<String, String> data);

    /**
     * 添加模型数据到响应中。
     *
     * @param data 要添加的模型数据，必须是ModelBase的子类实例。
     * @throws Exception 如果添加过程中发生错误，则抛出异常。
     */
    void addModelData(ModelBase data) throws Exception;
}
