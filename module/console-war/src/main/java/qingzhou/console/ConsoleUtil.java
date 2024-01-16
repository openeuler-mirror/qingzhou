package qingzhou.console;

import qingzhou.console.controller.rest.AccessControl;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.console.page.PageBackendService;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.KeyManager;
import qingzhou.framework.api.AddModel;
import qingzhou.framework.api.DeleteModel;
import qingzhou.framework.api.DownloadModel;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ConsoleUtil {// todo 临时工具类，后续考虑移除
    public static String ACTION_NAME_SERVER = "server";
    public static String ACTION_NAME_TARGET = "target";
    public static String ACTION_NAME_validate = "validate";
    private static Boolean disableUpload;
    private static Boolean disableDownload;

    private ConsoleUtil() {
    }

    public static void error(String msg, Throwable t) {
        ConsoleWarHelper.getLogger().error(msg, t);
    }

    public static void warn(String msg) {
        ConsoleWarHelper.getLogger().warn(msg);
    }

    public static String buildRequestUrl(HttpServletRequest servletRequest, HttpServletResponse response, Request request, String viewName, String actionName) {
        String url = servletRequest.getContextPath() + RESTController.REST_PREFIX + "/" + viewName + "/" + request.getAppName() + "/" + request.getModelName() + "/" + actionName;
        return response.encodeURL(url);
    }

    public static List<Map<String, String>> listModels(HttpServletRequest request, String targetType, String targetName, String appName, String modelName) {
       /* ModelManagerImpl modelManager = (ModelManagerImpl) getModelManager(appName); todo
        if (modelManager == null) {
            return new ArrayList<>();
        }

        Request qzRequest = buildRequest(request, targetType, targetName, appName, modelName, "list", null);
        ResponseImpl response = new ResponseImpl();
        try {
            modelManager.invokeAction(qzRequest, response);
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        return new ArrayList<>();
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

    /**
     * list.jsp 在使用
     */
    public static boolean isFilterSelect(Request request, int i) {
        final ModelManager modelManager = PageBackendService.getModelManager(request.getAppName());
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
        final ModelManager modelManager = PageBackendService.getModelManager(request.getAppName());
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

    public static boolean hasIDField(Request request) {
        try {
            final ModelManager modelManager = PageBackendService.getModelManager(request.getAppName());
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
        final ModelManager modelManager = PageBackendService.getModelManager(request.getAppName());
        if (modelManager == null) {
            return null;
        }
        if (modelAction != null) {
            String effectiveWhen = modelAction.effectiveWhen().trim();
            boolean effective = false;
            try {
                effective = Validator.isEffective(fieldName -> obj.get(fieldName), effectiveWhen);
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

    public static boolean isDisableUpload() {
        if (disableUpload == null) {
            disableUpload = ServerXml.get().isDisableUpload();
        }

        return disableUpload;
    }

    public static boolean isDisableDownload() {
        if (disableDownload == null) {
            disableDownload = ServerXml.get().isDisableDownload();
        }

        return disableDownload;
    }

    public static boolean isDisable(String action) {
        if (DownloadModel.ACTION_NAME_DOWNLOADLIST.equals(action)) {
            return isDisableDownload();
        } else {
            return false;
        }
    }

    public static Map<String, String> modelFieldEffectiveWhenMap(Request request) {
        final ModelManager modelManager = PageBackendService.getModelManager(request.getAppName());
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

    public static String getSubmitActionName(Request request) {
        boolean isEdit = Objects.equals("edit", request.getActionName());
        final ModelManager modelManager = PageBackendService.getModelManager(request.getAppName());
        if (modelManager == null) {
            return null;
        }
        for (String actionName : modelManager.getActionNames(request.getModelName())) {
            if (actionName.equals("update")) {
                if (isEdit) {
                    return "update";
                }
            } else if (actionName.equals("add")) {
                if (!isEdit) {
                    return "add";
                }
            }
        }
        return isEdit ? "update" : "add";// 兜底
    }

    /********************* 批量操作 start ************************/
    //公共操作列表
    public static boolean needOperationColumn(Request request, Response response, HttpSession session) throws Exception {
        final boolean[] needOperationColumn = {false};
        visitActions(request, response, session,
                obj -> {
                    needOperationColumn[0] = true;
                    return false;
                });
        return needOperationColumn[0];
    }

    private static void visitActions(Request request, Response response, HttpSession session, Visitor<ModelAction> visitor) throws Exception {
        visitActions(request, response, session, visitor, response.getDataList(), true);
    }

    private static void visitActions(Request request, Response response, HttpSession session, Visitor<ModelAction> visitor, List<Map<String, String>> datas, boolean checkEffective) throws Exception {
        final String appName = request.getAppName();
        final ModelManager modelManager = PageBackendService.getModelManager(appName);
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

                    if (!AccessControl.canAccess(request.getAppName(), request.getAppName() + "/" + modelName + "/" + actionName, request.getUserName())) { // todo
                        continue;
                    }

                    if (visitor.visitAndEnd(action)) {
                        break;
                    }
                }
            }
        }
    }

    public static ModelAction[] listCommonOps(Request request, Response response, HttpSession session) throws Exception {
        BatchVisitor batchVisitor = new BatchVisitor();
        visitActions(request, response, session, batchVisitor);
        return batchVisitor.modelActions.toArray(new ModelAction[0]);
    }

    public static ModelAction[] listModelBaseOps(Request request, Response response, HttpSession session, Map<String, String> obj) throws Exception {
        BatchVisitor batchVisitor = new BatchVisitor();
        visitActions(request, response, session, batchVisitor, new ArrayList<Map<String, String>>() {{
            add(obj);
        }}, true);
        return batchVisitor.modelActions.toArray(new ModelAction[0]);
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


    // jsp加密： 获取非对称算法密钥长度
    public static int getKeySize() { // login.jsp 使用
        return 1024;// 保持一致：PasswordCipherFactory.KeyGenerate
    }

    public static String encodeRedirectURL(HttpServletRequest request, HttpServletResponse response, String url) {
        return response.encodeURL(PageBackendService.encodeTarget(request, url));
    }

    public static String retrieveServletPathAndPathInfo(HttpServletRequest request) {
        return request.getServletPath() + (request.getPathInfo() != null ? request.getPathInfo() : "");
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
        CryptoService cryptoService = ConsoleWarHelper.getCryptoService();
        KeyManager keyManager = cryptoService.getKeyManager();
        File secureFile = ConsoleUtil.getSecureFile(ConsoleWarHelper.getDomain());
        return keyManager.getKeyPairOrElseInit(secureFile, ConsoleConstants.publicKeyName, ConsoleConstants.publicKeyName, ConsoleConstants.privateKeyName, null);
    }

    public static String decryptWithConsolePrivateKey(String input) {
        if (StringUtil.isBlank(input)) {
            return input;
        }
        try {
            File secureFile = ConsoleUtil.getSecureFile(ConsoleWarHelper.getDomain());
            KeyManager keyManager = ConsoleWarHelper.getCryptoService().getKeyManager();
            String pubKey = keyManager.getKeyPairOrElseInit(secureFile, ConsoleConstants.publicKeyName, ConsoleConstants.publicKeyName, ConsoleConstants.privateKeyName, null);
            String priKey = keyManager.getKeyPairOrElseInit(secureFile, ConsoleConstants.privateKeyName, ConsoleConstants.publicKeyName, ConsoleConstants.privateKeyName, null);

            return ConsoleWarHelper.getCryptoService().getPublicKeyCipher(pubKey, priKey).decryptWithPrivateKey(input);
        } catch (Exception e) {
            e.printStackTrace();
            return input;
        }
    }

    /**
     * 是否包含3个及以上相同或字典连续字符
     */
    private static boolean isContinuousChar(String password) {
        char[] chars = password.toCharArray();
        for (int i = 0; i < chars.length - 2; i++) {
            int n1 = chars[i];
            int n2 = chars[i + 1];
            int n3 = chars[i + 2];
            // 判断重复字符
            if (n1 == n2 && n1 == n3) {
                return true;
            }
            // 判断连续字符： 正序 + 倒序
            if ((n1 + 1 == n2 && n1 + 2 == n3) || (n1 - 1 == n2 && n1 - 2 == n3)) {
                return true;
            }
        }
        return false;
    }

    public static Map<String, Map<String, ModelField>> getGroupedModelFieldMap(Request request) {
        Map<String, Map<String, ModelField>> result = new LinkedHashMap<>();
        ModelManager manager = PageBackendService.getModelManager(request.getAppName());
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
}
