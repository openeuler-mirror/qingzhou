package qingzhou.deployer;

import qingzhou.api.Lang;
import qingzhou.api.Request;
import qingzhou.api.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestImpl implements Request, Cloneable {
    private transient final List<SessionParameterListener> sessionParameterListener = new ArrayList<>();
    private transient Response response = new ResponseImpl();

    private String appName;
    private String modelName;
    private String actionName;
    private String viewName;
    private String id;
    private String userName;
    private Lang lang;
    private final Map<String, String> parameters = new HashMap<>();
    private final Map<String, String> parametersInSession = new HashMap<>();

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
        return parameters.get(name);
    }

    @Override
    public Map<String, String> getParameters() {
        return parameters;
    }

    @Override
    public String getParameterInSession(String name) {
        return parametersInSession.get(name);
    }

    @Override
    public void setParameterInSession(String key, String val) {
        if (key == null) return;
        if (val == null) {
            parametersInSession.remove(key);
        }
        parametersInSession.put(key, val);

        sessionParameterListener.forEach(sessionParameterListener -> sessionParameterListener.onParameterSet(key, val));
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

    @Override
    public Response getResponse() {
        return response;
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

    public void removeParameter(String parameterName) {
        parameters.remove(parameterName);
    }

    public void setParameter(String parameterName, String parameterValue) {
        parameters.put(parameterName, parameterValue);
    }

    public void setParameters(Map<String, String> parameters) {
        if (parameters == null) return;

        RequestImpl.this.parameters.putAll(parameters);
    }

    public Map<String, String> getParametersInSession() {
        return parametersInSession;
    }

    public void addSessionParameterListener(SessionParameterListener sessionParameterListener) {
        this.sessionParameterListener.add(sessionParameterListener);
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    @Override
    public RequestImpl clone() throws CloneNotSupportedException {
        RequestImpl clone = (RequestImpl) super.clone();
        clone.parameters.putAll(this.parameters);
        return clone;
    }
}
