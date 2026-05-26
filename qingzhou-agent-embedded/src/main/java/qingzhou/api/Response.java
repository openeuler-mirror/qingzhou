package qingzhou.api;

public interface Response {
    Response success(boolean success);
    Response data(Object data);
    Response msg(String msg);
    Response msgLevel(MsgLevel msgLevel);
    Response status(int status);
    Response contentType(String contentType);
    Response header(String name, String value);

    enum MsgLevel {
        info, warn, error
    }
}