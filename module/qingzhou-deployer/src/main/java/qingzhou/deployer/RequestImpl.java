package qingzhou.deployer;

import qingzhou.api.Lang;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.registry.ModelInfo;

import java.util.*;

public class RequestImpl implements Request {
    private transient final List<SessionParameterListener> sessionParameterListener = new ArrayList<>();
    private transient Response response = new ResponseImpl();
    private transient ModelInfo cachedModelInfo = null;
    private transient Map<String, Response> responseList;

    private String appName;
    private String modelName;
    private String actionName;
    private String viewName;
    private String id;
    private String[] batchId;
    private String userName;
    private Lang lang;
    private byte[] byteParameter; // 发送上传的附件到远程实例上
    private final Map<String, String> nonModelParameters = new HashMap<>();
    private final Map<String, String> parameters = new HashMap<>();
    private final Map<String, String> parametersInSession = new HashMap<>();

    public RequestImpl() {
    }

    public RequestImpl(RequestImpl origin) {
        this.appName = origin.appName;
        this.modelName = origin.modelName;
        this.actionName = origin.actionName;
        this.viewName = origin.viewName;
        this.id = origin.id;
        this.userName = origin.userName;
        this.batchId = origin.batchId;
        this.lang = origin.lang;
        this.byteParameter = null; // 数据量大，且目前大部分业务并不需要它
    }

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
    public String[] getBatchId() {
        return batchId;
    }

    public void setBatchId(String[] batchId) {
        this.batchId = batchId;
    }

    @Override
    public String getNonModelParameter(String name) {
        return nonModelParameters.get(name);
    }

    @Override
    public String getParameter(String name) {
        return parameters.get(name);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameters.keySet());
    }

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

    public String removeParameter(String parameterName) {
        return parameters.remove(parameterName);
    }

    public void setNonModelParameter(String parameterName, String parameterValue) {
        nonModelParameters.put(parameterName, parameterValue);
    }

    public void setParameter(String parameterName, String parameterValue) {
        parameters.put(parameterName, parameterValue);
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

    public ModelInfo getCachedModelInfo() {
        return cachedModelInfo;
    }

    public void setCachedModelInfo(ModelInfo cachedModelInfo) {
        this.cachedModelInfo = cachedModelInfo;
    }

    public byte[] getByteParameter() {
        return byteParameter;
    }

    public void setByteParameter(byte[] byteParameter) {
        this.byteParameter = byteParameter;
    }

    public Map<String, Response> getResponseList() {
        return responseList;
    }

    public void setResponseList(Map<String, Response> responseList) {
        this.responseList = responseList;
    }
}
