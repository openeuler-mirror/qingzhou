package qingzhou.console.security;

import qingzhou.console.ConsoleUtil;
import qingzhou.console.controller.RestContext;
import qingzhou.api.console.FieldType;
import qingzhou.api.console.ModelField;
import qingzhou.api.console.ModelManager;
import qingzhou.console.RequestImpl;
import qingzhou.framework.pattern.Filter;
import qingzhou.console.util.StringUtil;

public class AsymmetricFilter implements Filter<RestContext> {
    @Override
    public boolean doFilter(RestContext context) throws Exception {
        RequestImpl request = (RequestImpl) context.request;
        String appName = ConsoleUtil.getAppName(request.getTargetType(), request.getTargetName());
        ModelManager modelManager = ConsoleUtil.getModelManager(appName);
        for (String fieldName : modelManager.getAllFieldNames(request.getModelName())) {
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
