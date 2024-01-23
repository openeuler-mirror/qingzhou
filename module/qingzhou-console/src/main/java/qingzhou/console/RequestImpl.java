package qingzhou.console;

import qingzhou.framework.api.Lang;
import qingzhou.framework.api.Request;

import java.io.Serializable;
import java.util.HashMap;

public class RequestImpl implements Request, Serializable, Cloneable {
    private String appName;
    private String modelName;
    private String actionName;
    private String viewName;
    private String id;
    private String userName;
    private Lang lang;
    private HashMap<String, String> parameters;

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

    @Override
    public String[] getParameterNames() {
        return parameters.keySet().toArray(new String[0]);
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

    @Override
    public Lang getI18nLang() {
        return lang;
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

    public void setI18nLang(Lang lang) {
        this.lang = lang;
    }

    public void updateParameter(String parameterName, String parameterValue) {
        parameters.put(parameterName, parameterValue);
    }

    public void setParameters(HashMap<String, String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public RequestImpl clone() throws CloneNotSupportedException {
        RequestImpl clone = (RequestImpl) super.clone();
        clone.parameters = (HashMap<String, String>) this.parameters.clone();
        return clone;
    }
}
