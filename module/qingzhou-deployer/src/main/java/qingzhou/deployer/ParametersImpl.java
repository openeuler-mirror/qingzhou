package qingzhou.deployer;

import qingzhou.api.Request;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParametersImpl implements Request.Parameters, Serializable {
    private transient final List<ParameterListener> parameterListener = new ArrayList<>();

    private final Map<String, String> parameters = new HashMap<>();

    @Override
    public String put(String key, String value) {
        String put = parameters.put(key, value);
        parameterListener.forEach(parameterListener -> parameterListener.onParameterPut(key, value));
        return put;
    }

    @Override
    public void putAll(Request.Parameters parameters) {
        ((ParametersImpl) parameters).parameters.forEach(this::put);
    }

    @Override
    public String get(String key) {
        return parameters.get(key);
    }

    @Override
    public String remove(String key) {
        return parameters.remove(key);
    }

    public void addParameterListener(ParameterListener parameterListener) {
        this.parameterListener.add(parameterListener);
    }
}
