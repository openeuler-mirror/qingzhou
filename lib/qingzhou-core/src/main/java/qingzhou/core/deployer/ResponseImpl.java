package qingzhou.core.deployer;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import qingzhou.api.MsgLevel;
import qingzhou.api.Response;
import qingzhou.core.deployer.impl.ParametersImpl;

public class ResponseImpl implements Response, Serializable {
    private boolean success = true;
    private boolean logout = false;
    private String msg;
    private MsgLevel msgLevel;
    private String contentType;
    private final Map<String, String> parameters = new HashMap<>();
    private final ParametersImpl parametersInSession = new ParametersImpl();
    private final Map<String, String> headers = new LinkedHashMap<>();
    private int statusCode = -1;
    private Serializable appData;
    private Serializable internalData;

    @Override
    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return this.success;
    }

    @Override
    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return this.msg;
    }

    @Override
    public void setMsgLevel(MsgLevel msgLevel) {
        this.msgLevel = msgLevel;
    }

    public MsgLevel getMsgLevel() {
        return this.msgLevel;
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    @Override
    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    @Override
    public void setStatusCode(int sc) {
        this.statusCode = sc;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    public Collection<String> getHeaderNames() {
        return headers.keySet();
    }

    public Serializable getAppData() {
        return appData;
    }

    @Override
    public void setData(Serializable data) {
        this.appData = data;
    }

    public void logout() {
        this.logout = true;
    }

    public boolean isLogout() {
        return logout;
    }

    public Serializable getInternalData() {
        return internalData;
    }

    public void setInternalData(Serializable internalData) {
        this.internalData = internalData;
    }

    public ParametersImpl getParametersInSession() {
        return parametersInSession;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }
}
