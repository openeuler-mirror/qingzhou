package qingzhou.console.controller.rest;

import qingzhou.api.FieldType;
import qingzhou.console.controller.SystemController;
import qingzhou.console.page.PageBackendService;
import qingzhou.deployer.RequestImpl;
import qingzhou.engine.util.pattern.Filter;
import qingzhou.registry.ModelFieldInfo;
import qingzhou.registry.ModelInfo;

public class ParameterFilter implements Filter<RestContext> {
    @Override
    public boolean doFilter(RestContext context) throws Exception {
        RequestImpl request = context.request;
        ModelInfo modelInfo = SystemController.getAppInfo(PageBackendService.getAppName(request)).getModelInfo(request.getModel());
        for (String fieldName : modelInfo.getFormFieldNames()) {
            ModelFieldInfo modelField = modelInfo.getModelFieldInfo(fieldName);
            if (modelField.getType().equals(FieldType.password.name())) {
                try {
                    String val = request.getParameter(fieldName);
                    String result = SystemController.decryptWithConsolePrivateKey(val, false);
                    if (result != null) { // 可能是空串
                        request.setParameter(fieldName, result.trim());
                    }
                } catch (Exception ignored) {
                }
            } else {
                // 其它字段，自动trim
                String val = request.getParameter(fieldName);
                if (val != null) {
                    request.setParameter(fieldName, val.trim());
                }
            }
        }

        return true;
    }
}