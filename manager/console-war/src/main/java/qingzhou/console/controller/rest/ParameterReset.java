package qingzhou.console.controller.rest;

import qingzhou.api.FieldType;
import qingzhou.console.controller.SystemController;
import qingzhou.console.page.PageBackendService;
import qingzhou.deployer.RequestImpl;
import qingzhou.engine.util.pattern.Filter;
import qingzhou.logger.Logger;
import qingzhou.registry.ModelFieldInfo;
import qingzhou.registry.ModelInfo;

public class ParameterReset implements Filter<RestContext> {
    @Override
    public boolean doFilter(RestContext context) throws Exception {
        RequestImpl request = context.request;
        ModelInfo modelInfo = PageBackendService.getAppInfo(PageBackendService.getAppName(request)).getModelInfo(request.getModel());
        for (String fieldName : modelInfo.getFormFieldNames()) {
            ModelFieldInfo modelField = modelInfo.getModelFieldInfo(fieldName);
            if (modelField.getType().equals(FieldType.password.name())) {
                try {
                    String val = request.getParameter(fieldName);
                    String result = decryptWithConsolePrivateKey(val);
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

    public static String decryptWithConsolePrivateKey(String input) {
        return decryptWithConsolePrivateKey(input, false);
    }

    public static String decryptWithConsolePrivateKey(String input, boolean ignoredEx) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        try {
            return SystemController.keyPairCipher.decryptWithPrivateKey(input);
        } catch (Exception e) {
            if (!ignoredEx) {
                SystemController.getService(Logger.class).warn("Decryption error", e);
            }
            return input;
        }
    }

    // js 加密： 获取非对称算法密钥长度
    public static int getKeySize() { // login.jsp 使用
        return 1024;
    }

    public static String getPublicKeyString() {
        return SystemController.getPublicKeyString();
    }
}
