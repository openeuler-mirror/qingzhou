package qingzhou.dto;

import java.util.HashMap;
import java.util.Map;

import qingzhou.api.Response;

public class ResponseImpl implements Response {
    // 响应数据
    private boolean success = true;
    private Object data;
    private String msg;
    private MessageLevel messageLevel;
    private int status;
    private String contentType;
    private final Map<String, String> headers = new HashMap<>();

    // 内部数据
    private boolean actionInvoked;

    @Override
    public Response success(boolean success) {
        this.success = success;
        return this;
    }

    @Override
    public Response data(Object data) {
        this.data = data;
        return this;
    }

    @Override
    public Response message(String message) {
        this.msg = message;
        return this;
    }

    @Override
    public Response messageLevel(MessageLevel messageLevel) {
        this.messageLevel = messageLevel;
        return this;
    }

    @Override
    public Response status(int status) {
        this.status = status;
        return this;
    }

    @Override
    public Response contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    @Override
    public Response header(String name, String value) {
        this.headers.put(name, value);
        return this;
    }

    @Override
    public Response error(String error) {
        this.message(error);
        this.messageLevel(MessageLevel.error);
        this.success(false);
        return this;
    }

    public boolean isSuccess() {
        return success;
    }

    public Object getData() {
        return data;
    }

    public String getMsg() {
        return msg;
    }

    public MessageLevel getMsgLevel() {
        return messageLevel;
    }

    public int getStatus() {
        return status;
    }

    public String getContentType() {
        return contentType;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public boolean isActionInvoked() {
        return actionInvoked;
    }

    public void setActionInvoked(boolean actionFound) {
        this.actionInvoked = actionFound;
    }
}
