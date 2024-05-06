package qingzhou.console.controller.rest;

import qingzhou.api.FieldType;
import qingzhou.console.RequestImpl;
import qingzhou.console.controller.SystemController;
import qingzhou.console.util.StringUtil;
import qingzhou.engine.util.pattern.Filter;
import qingzhou.logger.Logger;
import qingzhou.registry.ModelFieldInfo;
import qingzhou.registry.ModelInfo;

public class AsymmetricDecryptor implements Filter<RestContext> {
    @Override
    public boolean doFilter(RestContext context) throws Exception {
        RequestImpl request = context.request;
        ModelInfo modelInfo = SystemController.getAppInfo(request.getApp()).getModelInfo(request.getModel());
        for (String fieldName : modelInfo.getFormFieldNames()) {
            ModelFieldInfo modelField = modelInfo.getModelFieldInfo(fieldName);
            if (modelField.getType().equals(FieldType.password.name())) {
                String val = request.getParameter(fieldName);
                if (!StringUtil.isBlank(val)) {
                    try {
                        String result = decryptWithConsolePrivateKey(val);
                        if (StringUtil.notBlank(result)) {
                            request.setParameter(fieldName, result);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        return true;
    }

    public static String decryptWithConsolePrivateKey(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        try {
            return SystemController.keyPairCipher.decryptWithPrivateKey(input);
        } catch (Exception e) {
            SystemController.getService(Logger.class).warn("Decryption error", e);
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
