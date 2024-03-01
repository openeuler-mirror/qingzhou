package qingzhou.console.controller.rest;

import qingzhou.api.FieldType;
import qingzhou.api.ModelField;
import qingzhou.api.ModelManager;
import qingzhou.console.RestContext;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.framework.app.RequestImpl;
import qingzhou.framework.config.Config;
import qingzhou.framework.util.StringUtil;
import qingzhou.framework.util.pattern.Filter;
import qingzhou.serialization.ModelFieldData;

public class AsymmetricDecryptor implements Filter<RestContext> {
    @Override
    public boolean doFilter(RestContext context) throws Exception {
        RequestImpl request = (RequestImpl) context.request;
        ModelManager modelManager = ConsoleWarHelper.getAppStub(request.getAppName()).getModelManager();
        for (String fieldName : modelManager.getFieldNames(request.getModelName())) {
            ModelFieldData modelField = modelManager.getModelField(request.getModelName(), fieldName);
            if (modelField.type() == FieldType.password
                    || modelField.clientEncrypt()
            ) {
                String val = request.getParameter(fieldName);
                if (!StringUtil.isBlank(val)) {
                    try {
                        String result = decryptWithConsolePrivateKey(val);
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

    public static String decryptWithConsolePrivateKey(String input) {
        if (StringUtil.isBlank(input)) {
            return input;
        }
        try {
            String pubKey = ConsoleWarHelper.getConfig().getKey(Config.publicKeyName);
            String priKey = ConsoleWarHelper.getConfig().getKey(Config.privateKeyName);
            return ConsoleWarHelper.getCryptoService().getKeyPairCipher(pubKey, priKey).decryptWithPrivateKey(input);
        } catch (Exception e) {
            ConsoleWarHelper.getLogger().warn("Decryption error", e);
            return input;
        }
    }

    // jsp加密： 获取非对称算法密钥长度
    public static int getKeySize() { // login.jsp 使用
        return 1024;// 保持一致：PasswordCipherFactory.KeyGenerate
    }

    public static String getPublicKeyString() throws Exception {
        return ConsoleWarHelper.getConfig().getKey(Config.publicKeyName);
    }
}
