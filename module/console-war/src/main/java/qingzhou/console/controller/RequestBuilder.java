package qingzhou.console.controller;

import qingzhou.console.ConsoleUtil;
import qingzhou.console.login.LoginManager;
import qingzhou.api.console.data.Request;
import qingzhou.console.RequestImpl;
import qingzhou.console.util.Constants;
import qingzhou.console.util.StringUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public class RequestBuilder {
    private String targetType;
    private String targetName;
    private String appName;
    private String modelName;
    private String actionName;
    private String viewName;
    private String id;
    private HttpServletRequest request;
    private Map<String, String> fileAttachments;

    public String targetType() {
        return targetType;
    }

    public RequestBuilder targetType(String targetType) {
        this.targetType = targetType;
        return this;
    }

    public RequestBuilder targetName(String targetName) {
        this.targetName = targetName;
        return this;
    }

    public String modelName() {
        return modelName;
    }

    public RequestBuilder appName(String appName) {
        this.appName = appName;
        return this;
    }

    public RequestBuilder modelName(String modelName) {
        this.modelName = modelName;
        return this;
    }

    public String actionName() {
        return actionName;
    }

    public RequestBuilder actionName(String actionName) {
        this.actionName = actionName;
        return this;
    }

    public RequestBuilder viewName(String viewName) {
        this.viewName = viewName;
        return this;
    }

    public RequestBuilder id(String id) {
        this.id = id;
        return this;
    }

    public RequestBuilder request(HttpServletRequest request) {
        this.request = request;
        return this;
    }

    public RequestBuilder fileAttachments(Map<String, String> fileAttachments) {
        this.fileAttachments = fileAttachments;
        return this;
    }

    public Request build() {
        RequestImpl requestImpl = new RequestImpl(targetType, targetName, appName, modelName, actionName, viewName, id);

        String loginUser = LoginManager.getLoginUser(request.getSession(false));
        requestImpl.setLoginUser(loginUser);
        requestImpl.setUri(ConsoleUtil.retrieveServletPathAndPathInfo(request));
        requestImpl.setClientIp(request.getRemoteHost());

        List<String> names = new ArrayList<>();
        List<String> vals = new ArrayList<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String k = parameterNames.nextElement();
            String[] v = request.getParameterValues(k);
            if (v != null) {
                names.add(k);
                vals.add(String.join(Constants.DATA_SEPARATOR, v));
            }
        }
        if (fileAttachments != null) {
            requestImpl.setFileAttachments(fileAttachments);
            for (Map.Entry<String, String> entry : fileAttachments.entrySet()) {
                if (StringUtil.notBlank(entry.getValue())) {
                    int i = names.indexOf(entry.getKey());
                    if (i == -1) {
                        names.add(entry.getKey());
                        vals.add(entry.getValue());
                    } else {
                        names.set(i, entry.getValue());
                    }
                }
            }
        }
        requestImpl.setParameterNames(names.toArray(new String[0]));
        requestImpl.setParameterValues(vals.toArray(new String[0]));

        return requestImpl;
    }
}
