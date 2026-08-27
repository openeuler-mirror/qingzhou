package qingzhou.api;

public interface Response {
    Response success(boolean success);

    Response data(Object data);

    Response message(String message);

    Response messageLevel(MessageLevel messageLevel);

    Response status(int status);

    Response contentType(String contentType);

    Response header(String name, String value);

    /**
     * 等价于:
     * this.message(error);
     * this.messageLevel(Response.MessageLevel.error);
     * this.success(false);
     */
    Response error(String error);

    enum MessageLevel {
        info, warn, error
    }
}
