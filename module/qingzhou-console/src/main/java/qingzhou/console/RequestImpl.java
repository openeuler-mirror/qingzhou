package qingzhou.console;

import qingzhou.api.console.data.Request;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;

public class RequestImpl implements Request, Serializable, Cloneable {
    private String targetType;
    private String targetName;
    private final String appName;
    private String modelName;
    private String actionName;
    private final String viewName;
    private String id;
    private String[] parameterNames;
    private String[] parameterValues;
    private String clientIp;
    private String uri;
    private String loginUser;
    private Map<String, String> fileAttachments;

    public RequestImpl(String targetType, String targetName, String appName, String modelName, String actionName, String viewName, String id) {
        this.targetType = targetType;
        this.targetName = targetName;
        this.appName = appName;
        this.modelName = modelName;
        this.actionName = actionName;
        this.viewName = viewName;
        this.id = id;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    @Override
    public String getAppName() {
        return appName;
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public String getActionName() {
        return actionName;
    }

    @Override
    public String getViewName() {
        return viewName;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getParameter(String parameterName) {
        if (parameterNames == null) return null;
        int index = parameterIndex(parameterName);
        if (index == -1) {
            return null;
        }
        return parameterValues[index];
    }

    private int parameterIndex(String parameterName) {
        for (int i = 0; i < parameterNames.length; i++) {
            if (parameterNames[i].equals(parameterName)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String getClientIp() {
        return clientIp;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public String getLoginUser() {
        return loginUser;
    }

    public void updateParameter(String parameterName, String parameterValue) {
        int i = parameterIndex(parameterName);
        if (i != -1) {
            parameterValues[i] = parameterValue;
        }
    }

    public void removeParameter(String parameterName) {
        if (parameterNames == null) return;
        int index = parameterIndex(parameterName);
        if (index == -1) {
            return;
        }
        parameterNames = removeIndexInArray(parameterNames, index);
        parameterValues = removeIndexInArray(parameterValues, index);
    }

    private String[] removeIndexInArray(String[] array, int index) {
        String[] a = Arrays.copyOfRange(array, 0, index);
        String[] b = Arrays.copyOfRange(array, index + 1, array.length);
        String[] merge = new String[a.length + b.length];
        System.arraycopy(a, 0, merge, 0, a.length);
        System.arraycopy(b, 0, merge, a.length, b.length);
        return merge;
    }

    public void setParameterNames(String[] parameterNames) {
        this.parameterNames = parameterNames;
    }

    public void setParameterValues(String[] parameterValues) {
        this.parameterValues = parameterValues;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setLoginUser(String loginUser) {
        this.loginUser = loginUser;
    }

    public Map<String, String> getFileAttachments() {
        return fileAttachments;
    }

    public void setFileAttachments(Map<String, String> fileAttachments) {
        this.fileAttachments = fileAttachments;
    }

    @Override
    public RequestImpl clone() throws CloneNotSupportedException {
        return (RequestImpl) super.clone();
    }
}
