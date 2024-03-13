package qingzhou.framework.console;

import qingzhou.api.Lang;
import qingzhou.api.Request;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class RequestImpl implements Request, Serializable, Cloneable {
    private String manageType;
    private String appName;
    private String modelName;
    private String actionName;
    private String viewName;
    private String id;
    private String userName;
    private Lang lang;
    private Map<String, String[]> parameters;

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
        String[] values = this.parameters.getOrDefault(parameterName, new String[0]);
        return values.length == 0 ? null : values[0];
    }

    @Override
    public Map<String, String[]> getParameters() {
        return this.parameters;
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
        parameters.put(parameterName, new String[]{ parameterValue });
    }

    public void setParameters(Map<String, String> parameters) {
        Map<String, String[]> temp = new HashMap<>(32);
        if (parameters != null) {
            parameters.forEach((k, v) -> temp.put(k, new String[]{v}));
        }
        this.parameters = temp;
    }

    public String getManageType() {
        return manageType;
    }

    public void setManageType(String manageType) {
        this.manageType = manageType;
    }

    @Override
    public RequestImpl clone() throws CloneNotSupportedException {
        RequestImpl clone = (RequestImpl) super.clone();
        clone.parameters = this.parameters.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        return clone;
    }
}
