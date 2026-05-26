package qingzhou.dto;

import java.util.HashMap;
import java.util.Map;

import qingzhou.api.Response;

public class ResponseImpl implements Response {
    private boolean success = true;
    private Object data;
    private String msg;
    private MsgLevel msgLevel;
    private int status;
    private String contentType;
    private final Map<String, String> headers = new HashMap<>();
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
    public Response msg(String msg) {
        this.msg = msg;
        return this;
    }

    @Override
    public Response msgLevel(MsgLevel msgLevel) {
        this.msgLevel = msgLevel;
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

    public boolean isSuccess() {
        return success;
    }

    public Object getData() {
        return data;
    }

    public String getMsg() {
        return msg;
    }

    public MsgLevel getMsgLevel() {
        return msgLevel;
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

    public void setActionInvoked(boolean actionInvoked) {
        this.actionInvoked = actionInvoked;
    }
}