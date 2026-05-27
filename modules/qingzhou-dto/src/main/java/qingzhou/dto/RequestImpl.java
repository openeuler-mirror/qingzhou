package qingzhou.dto;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import qingzhou.api.Request;
import qingzhou.dto.meta.annotation.Model;

public class RequestImpl implements Request {
    private transient ResponseImpl response = new ResponseImpl();
    private transient Model currentModel;

    private String instance;
    private String app;
    private String model;
    private String action;
    private String id;
    private final Map<String, String> parameters = new HashMap<>();
    private final Set<String> uploadFileFields = new HashSet<>();

    @Override
    public ResponseImpl getResponse() {
        return response;
    }

    @Override
    public String getInstance() {
        return instance;
    }

    @Override
    public String getApp() {
        return app;
    }

    @Override
    public String getModel() {
        return model;
    }

    @Override
    public String getAction() {
        return action;
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

    public Set<String> getUploadFileFields() {
        return uploadFileFields;
    }

    public void setResponse(ResponseImpl response) {
        this.response = response;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Model getCurrentModel() {
        return currentModel;
    }

    public void setCurrentModel(Model currentModel) {
        this.currentModel = currentModel;
    }
}
