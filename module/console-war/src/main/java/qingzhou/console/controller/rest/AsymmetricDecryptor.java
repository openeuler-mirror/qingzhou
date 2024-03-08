package qingzhou.console.controller.rest;

import qingzhou.api.FieldType;
import qingzhou.api.metadata.ModelFieldData;
import qingzhou.api.metadata.ModelManager;
import qingzhou.console.controller.SystemController;
import qingzhou.console.page.PageBackendService;
import qingzhou.framework.console.RequestImpl;
import qingzhou.framework.config.Config;
import qingzhou.framework.util.StringUtil;
import qingzhou.framework.util.pattern.Filter;

public class AsymmetricDecryptor implements Filter<RestContext> {
    @Override
    public boolean doFilter(RestContext context) throws Exception {
        RequestImpl request = context.request;
        ModelManager modelManager = SystemController.getAppMetadata(request).getModelManager();
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
            String pubKey = SystemController.getConfig().getKey(Config.publicKeyName);
            String priKey = SystemController.getConfig().getKey(Config.privateKeyName);
            return SystemController.getCryptoService().getKeyPairCipher(pubKey, priKey).decryptWithPrivateKey(input);
        } catch (Exception e) {
            SystemController.getLogger().warn("Decryption error", e);
            return input;
        }
    }

    // jsp加密： 获取非对称算法密钥长度
    public static int getKeySize() { // login.jsp 使用
        return 1024;// 保持一致：PasswordCipherFactory.KeyGenerate
    }

    public static String getPublicKeyString() throws Exception {
        return SystemController.getConfig().getKey(Config.publicKeyName);
    }
}
