package qingzhou.console.impl;

import qingzhou.crypto.CryptoService;
import qingzhou.crypto.KeyManager;
import qingzhou.framework.AppInfo;
import qingzhou.framework.AppManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.AddModel;
import qingzhou.framework.api.ConsoleContext;
import qingzhou.framework.api.EditModel;
import qingzhou.framework.api.FieldType;
import qingzhou.framework.api.ListModel;
import qingzhou.framework.api.ModelAction;
import qingzhou.framework.api.ModelField;
import qingzhou.framework.api.ModelManager;
import qingzhou.framework.api.Option;
import qingzhou.framework.api.Options;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;
import qingzhou.framework.console.ConsoleConstants;
import qingzhou.framework.console.I18n;
import qingzhou.framework.console.Validator;
import qingzhou.framework.pattern.Visitor;
import qingzhou.framework.util.ExceptionUtil;
import qingzhou.framework.util.FileUtil;
import qingzhou.framework.util.StringUtil;
import qingzhou.logger.Logger;
import qingzhou.logger.LoggerService;
import qingzhou.serializer.Serializer;
import qingzhou.serializer.SerializerService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ConsoleWarHelper {
    static FrameworkContext fc;

    public static AppManager getAppInfoManager() {
        return fc.getAppManager();
    }

    public static Serializer getSerializer() {
        return fc.getService(SerializerService.class).getSerializer();
    }

    public static CryptoService getCryptoService() {
        return fc.getService(CryptoService.class);
    }

    public static File getCache() {
        return fc.getCache();
    }

    public static File getLibDir() {
        return fc.getLib();
    }

    public static File getDomain() {
        return fc.getDomain();
    }

    public static File getHome() {
        return fc.getHome();
    }

    public static Logger getLogger() {
        return fc.getService(LoggerService.class).getLogger();
    }

    public static ConsoleContext getMasterConsoleContext() {
        AppInfo appInfo = fc.getAppManager().getAppInfo(ConsoleConstants.MASTER_APP_NAME);
        return appInfo.getAppContext().getConsoleContext();
    }

    public static ModelManager getModelManager(String appName) {
        return getAppInfoManager().getAppInfo(appName).getAppContext().getConsoleContext().getModelManager();
    }

    public static void multiSelectGroup(LinkedHashMap<String, String> groupDes,
                                        LinkedHashMap<String, LinkedHashMap<String, String>> groupedMap,
                                        Options optionsManager) {
        LinkedHashMap<String, LinkedHashMap<String, String>> tempGroup = new LinkedHashMap<>();
        LinkedHashMap<String, String> twoGroup = new LinkedHashMap<>();
        for (Option option : optionsManager.options()) {
            String value = option.value();
            String[] groupData = value.split(ConsoleConstants.OPTION_GROUP_SEPARATOR);
            String desc = I18n.getString(option.i18n());
            if (groupData.length == 1) {
                groupDes.putIfAbsent(value, desc);
            } else if (groupData.length == 2) {
                LinkedHashMap<String, String> items = tempGroup.computeIfAbsent(groupData[0], k -> new LinkedHashMap<>());
                items.putIfAbsent(groupData[0], desc);
                twoGroup.putIfAbsent(value, desc);
            } else if (groupData.length == 3) {
                LinkedHashMap<String, String> items = groupedMap.computeIfAbsent(groupData[0] + ConsoleConstants.OPTION_GROUP_SEPARATOR + groupData[1], k -> new LinkedHashMap<>());
                items.put(value, desc);
            }
        }
        if (groupedMap.size() > 0) {
            groupDes.putAll(twoGroup);
        } else {
            groupedMap.putAll(tempGroup);
        }
    }

    public static Map<String, Map<String, ModelField>> getGroupedModelFieldMap(Request request) {
        Map<String, Map<String, ModelField>> result = new LinkedHashMap<>();
        ModelManager manager = getAppInfoManager().getAppInfo(request.getAppName()).getAppContext().getConsoleContext().getModelManager();
        String modelName = request.getModelName();
        for (String groupName : manager.getGroupNames(modelName)) {
            Map<String, ModelField> map = new LinkedHashMap<>();
            String[] fieldNamesByGroup = manager.getFieldNamesByGroup(modelName, groupName);
            for (String f : fieldNamesByGroup) {
                ModelField modelField = manager.getModelField(modelName, f);
                map.put(f, modelField);
            }
            result.put(groupName, map);
        }

        return result;
    }

    public static String decryptWithConsolePrivateKey(String input) {
        if (StringUtil.isBlank(input)) {
            return input;
        }
        try {
            File secureFile = getSecureFile(getDomain());
            KeyManager keyManager = getCryptoService().getKeyManager();
            String pubKey = keyManager.getKeyPairOrElseInit(secureFile, ConsoleConstants.publicKeyName, ConsoleConstants.publicKeyName, ConsoleConstants.privateKeyName, null);
            String priKey = keyManager.getKeyPairOrElseInit(secureFile, ConsoleConstants.privateKeyName, ConsoleConstants.publicKeyName, ConsoleConstants.privateKeyName, null);

            return getCryptoService().getPublicKeyCipher(pubKey, priKey).decryptWithPrivateKey(input);
        } catch (Exception e) {
            e.printStackTrace();
            return input;
        }
    }

    public static synchronized File getSecureFile(File domain) throws IOException {
        File secureDir = FileUtil.newFile(domain, "data", "secure");
        FileUtil.mkdirs(secureDir);
        File secureFile = FileUtil.newFile(secureDir, "secure");
        if (!secureFile.exists()) {
            if (!secureFile.createNewFile()) {
                throw ExceptionUtil.unexpectedException(secureFile.getPath());
            }
        }

        return secureFile;
    }

    public static String getPublicKeyString() throws Exception {
        CryptoService cryptoService = getCryptoService();
        KeyManager keyManager = cryptoService.getKeyManager();
        File secureFile = getSecureFile(getDomain());
        return keyManager.getKeyPairOrElseInit(secureFile, ConsoleConstants.publicKeyName, ConsoleConstants.publicKeyName, ConsoleConstants.privateKeyName, null);
    }

    // jsp加密： 获取非对称算法密钥长度
    public static int getKeySize() { // login.jsp 使用
        return 1024;// 保持一致：PasswordCipherFactory.KeyGenerate
    }


    public static String getSubmitActionName(Request request) {
        boolean isEdit = Objects.equals(EditModel.ACTION_NAME_EDIT, request.getActionName());
        final ModelManager modelManager = getAppInfoManager().getAppInfo(request.getAppName()).getAppContext().getConsoleContext().getModelManager();
        if (modelManager == null) {
            return null;
        }
        for (String actionName : modelManager.getActionNames(request.getModelName())) {
            if (actionName.equals(EditModel.ACTION_NAME_UPDATE)) {
                if (isEdit) {
                    return EditModel.ACTION_NAME_UPDATE;
                }
            } else if (actionName.equals(AddModel.ACTION_NAME_ADD)) {
                if (!isEdit) {
                    return AddModel.ACTION_NAME_ADD;
                }
            }
        }
        return isEdit ? EditModel.ACTION_NAME_UPDATE : AddModel.ACTION_NAME_ADD;// 兜底
    }

    public static boolean hasIDField(Request request) {
        try {
            final ModelManager modelManager = getModelManager(request.getAppName());
            if (modelManager == null) {
                return false;
            }
            ModelField modelField = modelManager.getModelField(request.getModelName(), ListModel.FIELD_NAME_ID);
            if (modelField != null) {
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public static String isActionEffective(Request request, Map<String, String> obj, ModelAction modelAction) {
        final ModelManager modelManager = getModelManager(request.getAppName());
        if (modelManager == null) {
            return null;
        }
        if (modelAction != null) {
            String effectiveWhen = modelAction.effectiveWhen().trim();
            boolean effective = false;
            try {
                effective = Validator.isEffective(obj::get, effectiveWhen);
            } catch (Exception ignored) {
            }
            if (!effective) {
                return String.format(
                        I18n.getString(ConsoleConstants.MASTER_APP_NAME, "validator.ActionEffective.notsupported"),
                        I18n.getString(request.getAppName(), "model.action." + request.getModelName() + "." + request.getActionName()),// todo
                        effectiveWhen);
            }
        }
        return null;
    }

    public static void error(String msg, Throwable t) {
        getLogger().error(msg, t);
    }

    public static void warn(String msg) {
        getLogger().warn(msg);
    }

    public static Map<String, String> modelFieldEffectiveWhenMap(Request request) {
        final ModelManager modelManager = getModelManager(request.getAppName());
        if (modelManager == null) {
            return new HashMap<>();
        }
        Map<String, String> result = new HashMap<>();
        String modelName = request.getModelName();
        for (String fieldName : modelManager.getFieldNames(modelName)) {
            ModelField modelField = modelManager.getModelField(modelName, fieldName);
            String condition = modelField.effectiveWhen();
            if (StringUtil.notBlank(condition)) {
                result.put(fieldName, condition);
            }
        }

        return result;
    }

    /**
     * list.jsp 在使用
     */
    public static boolean isFilterSelect(Request request, int i) {
        final ModelManager modelManager = getModelManager(request.getAppName());
        if (modelManager == null) {
            return false;
        }
        String modelName = request.getModelName();
        String[] allFieldNames = modelManager.getFieldNames(modelName);
        ModelField modelField = modelManager.getModelField(modelName, allFieldNames[i]);
        FieldType fieldType = modelField.type();
        return fieldType == FieldType.radio || fieldType == FieldType.bool || fieldType == FieldType.select || fieldType == FieldType.groupedMultiselect || fieldType == FieldType.checkbox || fieldType == FieldType.sortableCheckbox;
    }

    public static boolean isFieldReadOnly(Request request, String fieldName) {
        final ModelManager modelManager = getModelManager(request.getAppName());
        if (modelManager == null) {
            return false;
        }
        ModelField modelField = modelManager.getModelField(request.getModelName(), fieldName);
        if (modelField.maxLength() < 1) {
            return true;
        }
        if (modelField.disableOnCreate() && modelField.disableOnEdit()) {
            return true;
        }

        return false;
    }


    /********************* 批量操作 start ************************/
    public static ModelAction[] listCommonOps(Request request, Response response) throws Exception {
        BatchVisitor batchVisitor = new BatchVisitor();
        visitActions(request, response, batchVisitor);
        return batchVisitor.modelActions.toArray(new ModelAction[0]);
    }

    public static ModelAction[] listModelBaseOps(Request request, Response response, Map<String, String> obj) throws Exception {
        BatchVisitor batchVisitor = new BatchVisitor();
        visitActions(request, response, batchVisitor, new ArrayList<Map<String, String>>() {{
            add(obj);
        }}, true);
        return batchVisitor.modelActions.toArray(new ModelAction[0]);
    }

    //公共操作列表
    public static boolean needOperationColumn(Request request, Response response) throws Exception {
        final boolean[] needOperationColumn = {false};
        visitActions(request, response,
                obj -> {
                    needOperationColumn[0] = true;
                    return false;
                });
        return needOperationColumn[0];
    }

    private static void visitActions(Request request, Response response, Visitor<ModelAction> visitor) throws Exception {
        visitActions(request, response, visitor, response.getDataList(), true);
    }

    private static void visitActions(Request request, Response response, Visitor<ModelAction> visitor, List<Map<String, String>> datas, boolean checkEffective) throws Exception {
        final String appName = request.getAppName();
        final ModelManager modelManager = getModelManager(appName);
        if (modelManager == null) {
            return;
        }
        final String modelName = request.getModelName();
        boolean hasId = hasIDField(request);
        if (hasId && !response.getDataList().isEmpty()) {
            for (String ac : modelManager.getActionNames(modelName)) {
                ModelAction action = modelManager.getModelAction(modelName, ac);
                for (Map<String, String> data : datas) {
                    if (checkEffective && isActionEffective(request, data, action) != null) {
                        continue;
                    }

                    final String actionName = action.name();
                    if (EditModel.ACTION_NAME_EDIT.equals(actionName)) {
                        continue;
                    }

                    if (!action.showToList() || action.showToListHead()) {
                        continue;
                    }

                    if (visitor.visitAndEnd(action)) {
                        break;
                    }
                }
            }
        }
    }

    private static class BatchVisitor implements Visitor<ModelAction> {
        List<ModelAction> modelActions = new ArrayList<>();

        @Override
        public boolean visitAndEnd(ModelAction action) {
            if (!action.supportBatch()) {
                return true;
            }

            if (!modelActions.contains(action)) {
                modelActions.add(action);
            }

            return false;
        }
    }
    /********************* 批量操作 end ************************/

    private ConsoleWarHelper() {
    }
}
