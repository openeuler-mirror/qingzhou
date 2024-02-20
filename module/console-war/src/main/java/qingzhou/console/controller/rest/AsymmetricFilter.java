package qingzhou.console.controller.rest;

import qingzhou.console.page.PageBackendService;
import qingzhou.framework.app.RequestImpl;
import qingzhou.framework.api.FieldType;
import qingzhou.framework.api.ModelField;
import qingzhou.framework.api.ModelManager;
import qingzhou.framework.pattern.Filter;
import qingzhou.framework.util.StringUtil;

public class AsymmetricFilter implements Filter<RestContext> {
    @Override
    public boolean doFilter(RestContext context) throws Exception {
        RequestImpl request = (RequestImpl) context.request;
        String appName = request.getAppName();
        ModelManager modelManager = PageBackendService.getModelManager(appName);
        for (String fieldName : modelManager.getFieldNames(request.getModelName())) {
            ModelField modelField = modelManager.getModelField(request.getModelName(), fieldName);
            if (modelField.type() == FieldType.password
                    || modelField.clientEncrypt()
            ) {
                String val = request.getParameter(fieldName);
                if (!StringUtil.isBlank(val)) {
                    try {
                        String result = PageBackendService.decryptWithConsolePrivateKey(val);
                        if (StringUtil.notBlank(result)) {
                            request.updateParameter(fieldName, result);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        return true;
    }
}
