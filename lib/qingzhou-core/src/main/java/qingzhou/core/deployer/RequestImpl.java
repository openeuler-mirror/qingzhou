package qingzhou.core.deployer;

import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import qingzhou.api.Lang;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.core.deployer.impl.ParametersImpl;
import qingzhou.core.registry.ModelInfo;
import qingzhou.engine.util.Utils;

public class RequestImpl implements Request, Serializable {
    private transient Response response = new ResponseImpl();
    private transient ModelInfo cachedModelInfo = null;
    private transient Map<String, Response> invokeOnInstances;

    private String appName;
    private String modelName;
    private String actionName;
    private String viewName;
    private String id;
    private String[] batchId;
    private String userName;
    private Lang lang;
    private byte[] httpBody;
    private final Map<String, String> parameters = new HashMap<>();
    private final Map<String, String> headers = new HashMap<>();
    private final ParametersImpl parametersForSession = new ParametersImpl();
    private final ParametersImpl parametersForSubMenu = new ParametersImpl();

    private byte[] byteParameter; // 发送上传的附件到远程实例上

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
        this.httpBody = origin.httpBody;
        this.parameters.putAll(origin.parameters);
        this.headers.putAll(origin.headers);
        this.parametersForSubMenu.putAll(origin.parametersForSubMenu);
        this.parametersForSession.putAll(origin.parametersForSession);

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
    public String getParameter(String name) {
        return parameters.get(name);
    }

    @Override
    public <T> T getParameterAsObject(Class<T> objectType) throws Exception {
        T parameterObject = objectType.newInstance();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            Utils.setPropertyToObj(parameterObject, entry.getKey(), entry.getValue());
        }
        return parameterObject;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameters.keySet());
    }

    @Override
    public Parameters parametersForSession() {
        return parametersForSession;
    }

    @Override
    public Parameters parametersForSubMenu() {
        return parametersForSubMenu;
    }

    @Override
    public String getHeader(String name) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(headers.keySet());
    }

    @Override
    public byte[] getHttpBody() {
        return httpBody;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, String> getParameters() {
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

    public void setLang(Lang lang) {
        this.lang = lang;
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

    public Map<String, Response> getInvokeOnInstances() {
        return invokeOnInstances;
    }

    public void setInvokeOnInstances(Map<String, Response> invokeOnInstances) {
        this.invokeOnInstances = invokeOnInstances;
    }

    public void addSessionParameterListener(ParameterListener parameterListener) {
        this.parametersForSession.addParameterListener(parameterListener);
    }

    public void setHttpBody(byte[] httpBody) {
        this.httpBody = httpBody;
    }
}
