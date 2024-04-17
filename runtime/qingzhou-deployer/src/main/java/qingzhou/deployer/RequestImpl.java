package qingzhou.deployer;

import qingzhou.api.Lang;
import qingzhou.api.Request;

import java.util.HashMap;
import java.util.Map;

public class RequestImpl implements Request, Cloneable {
    private String manageType;
    private String appName;
    private String modelName;
    private String actionName;
    private String viewName;
    private String id;
    private String userName;
    private Lang lang;

    /**
     * The request parameters for this request.
     */
    protected Map<String, String[]> parameters = new HashMap<>();

    @Override
    public String getApp() {
        return appName;
    }

    @Override
    public String getModel() {
        return modelName;
    }

    @Override
    public String getAction() {
        return actionName;
    }

    @Override
    public String getView() {
        return viewName;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getParameter(String name) {
        String[] value = parameters.get(name);
        if (value == null) {
            return null;
        }
        return value[0];
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return parameters;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getUser() {
        return userName;
    }

    @Override
    public Lang getLang() {
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

    public void setParameter(String parameterName, String parameterValue) {
        parameters.put(parameterName, new String[]{parameterValue});
    }

    public void setParameters(Map<String, String> parameters) {
        if (parameters == null) return;

        parameters.forEach((s, s2) -> RequestImpl.this.parameters.put(s, new String[]{s2}));
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
        clone.parameters.putAll(this.parameters);
        return clone;
    }
}
