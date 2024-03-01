package qingzhou.app.mbean;

import qingzhou.api.Lang;
import qingzhou.app.Controller;
import qingzhou.framework.app.RequestImpl;
import qingzhou.framework.app.ResponseImpl;
import qingzhou.framework.util.ArrayUtil;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerImpl implements ServerImplMBean {
    private final String[] supportAction = new String[]{"show", "list", "monitor"};

    private List<Map<String, String>> callServerMethod(String appName, String modelName, String actionName, String name) throws Exception {
        if (modelName.contains(".")) {
            throw new IllegalArgumentException("modelName parameter is illegal");
        }
        if (!ArrayUtil.contains(supportAction, actionName)) {
            throw new IllegalArgumentException("actionName only show, list, monitor are supported");
        }
        try {
            RequestImpl request = new RequestImpl();
            request.setViewName("json");
            request.setManageType("app");
            request.setAppName(appName);
            request.setModelName(modelName);
            request.setActionName(actionName);
            request.setI18nLang(Lang.zh);
            request.setId(name);
            request.setUserName("qingzhou");
            request.setParameters(new HashMap<>());

            ResponseImpl response = new ResponseImpl();
            Controller.appManager.getApp(appName).invoke(request, response);
            return response.getDataList();
        } catch (ClassNotFoundException e) {
            throw new InvalidParameterException("modelName parameter [" + modelName + "] is invalid");
        }
    }

    @Override
    public List<Map<String, String>> list(String appName, String modelName) throws Exception {
        return callServerMethod(appName, modelName, "list", null);
    }

    @Override
    public Map<String, String> show(String appName, String modelName, String name) throws Exception {
        List<Map<String, String>> result = callServerMethod(appName, modelName, "show", name);
        if (result.isEmpty()) {
            return null;
        } else {
            return result.get(0);
        }
    }

    @Override
    public Map<String, String> monitor(String appName, String modelName, String name) throws Exception {
        List<Map<String, String>> result = callServerMethod(appName, modelName, "monitor", name);
        if (result.isEmpty()) {
            return null;
        } else {
            return result.get(0);
        }
    }
}
