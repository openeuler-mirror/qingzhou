package qingzhou.api;

public interface Response {
    /**
     * 表示本次请求是否正确返回数据
     */
    Response success(boolean success);

    /**
     * 响应数据，此数据会被序列化为 json 格式
     */
    Response data(Object data);

    /**
     * 如果没有返回数据，可给出返回消息
     */
    Response msg(String msg);

    /**
     * 可指定返回消息的异常级别
     */
    Response msgLevel(MsgLevel msgLevel);

    /**
     * 设置 http 协议响应码
     */
    Response status(int status);

    /**
     * 设置 http 协议响应头：content-type
     */
    Response contentType(String contentType);

    /**
     * 设置 http 协议响应头
     */
    Response header(String name, String value);

    enum MsgLevel {
        info, warn, error
    }
}
