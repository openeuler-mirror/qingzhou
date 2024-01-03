package qingzhou.console.controller.rest;

import qingzhou.framework.api.FieldType;
import qingzhou.framework.api.ModelField;
import qingzhou.framework.api.ModelManager;
import qingzhou.console.ConsoleUtil;
import qingzhou.framework.console.RequestImpl;
import qingzhou.framework.util.StringUtil;
import qingzhou.framework.pattern.Filter;

public class AsymmetricFilter implements Filter<RestContext> {
    @Override
    public boolean doFilter(RestContext context) throws Exception {
        RequestImpl request = (RequestImpl) context.request;
        String appName = request.getAppName();
        ModelManager modelManager = ConsoleUtil.getModelManager(appName);
        for (String fieldName : modelManager.getFieldNames(request.getModelName())) {
            ModelField modelField = modelManager.getModelField(request.getModelName(), fieldName);
            if (modelField.type() == FieldType.password
                    || modelField.clientEncrypt()
            ) {
                String val = request.getParameter(fieldName);
                if (!StringUtil.isBlank(val)) {
                    try {
                        String result = ConsoleUtil.decryptWithConsolePrivateKey(val);
                        request.updateParameter(fieldName, result);
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        return true;
    }
}
