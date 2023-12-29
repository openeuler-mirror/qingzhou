package qingzhou.console;

import qingzhou.api.console.data.Request;

import java.io.Serializable;
import java.util.Map;

public class RequestImpl implements Request, Serializable, Cloneable {
    private String testName;
    private String appName;
    private String modelName;
    private String actionName;
    private String viewName;
    private String id;
    private String userName;
    private Map<String, String> parameters;

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
        return parameters.get(parameterName);
    }

    @Override
    public String getUserName() {
        return userName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void updateParameter(String parameterName, String parameterValue) {
        parameters.put(parameterName, parameterValue);
    }

    public void removeParameter(String parameterName) {
        parameters.remove(parameterName);
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public RequestImpl clone() throws CloneNotSupportedException {
        return (RequestImpl) super.clone();
    }
}
