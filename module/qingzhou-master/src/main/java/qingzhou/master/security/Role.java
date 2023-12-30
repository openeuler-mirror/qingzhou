package qingzhou.master.security;

import qingzhou.api.*;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.console.impl.ServerXml;
import qingzhou.framework.api.*;
import qingzhou.master.MasterModelBase;
import qingzhou.master.impl.Controller;
import qingzhou.master.product.AppVersion;
import qingzhou.master.service.Cluster;
import qingzhou.master.service.Node;

import java.util.*;

@Model(name = "role", icon = "group",
        menuName = "Security", menuOrder = 2,
        nameI18n = {"角色", "en:Role"},
        infoI18n = {"管理登录和操作服务器的系统管理员角色。",
                "en:Manage the user role for logging in and operating the server."})
public class Role extends MasterModelBase implements AddModel {
    // 自动追加权限
    public static final Map<String, String[]> cascadeActions = Collections.unmodifiableMap(new HashMap<String, String[]>() {{
        put(AddModel.ACTION_NAME_ADD, new String[]{AddModel.ACTION_NAME_CREATE});
        put(ShowModel.ACTION_NAME_SHOW, new String[]{EditModel.ACTION_NAME_EDIT, ListModel.ACTION_NAME_LIST});// 如 ejb 此类的 服务类型的modle，编辑就是查看
        put(EditModel.ACTION_NAME_UPDATE, new String[]{EditModel.ACTION_NAME_EDIT, ListModel.ACTION_NAME_LIST});
        put(DownloadModel.ACTION_NAME_DOWNLOADFILE, new String[]{DownloadModel.ACTION_NAME_DOWNLOADLIST, EditModel.ACTION_NAME_EDIT, ListModel.ACTION_NAME_LIST});
    }});
    public static final String[] commonActions = {
            qingzhou.framework.api.Constants.MASTER_APP_NAME + "/" + Constants.MODEL_NAME_index + "/" + Constants.ACTION_NAME_INDEX,
            qingzhou.framework.api.Constants.MASTER_APP_NAME + "/" + Constants.MODEL_NAME_home + "/home"
    };

    static {
        ConsoleContext consoleContext = ConsoleWarHelper.getMasterAppConsoleContext();
        if (consoleContext != null) {
            consoleContext.addI18N("optionsValidationMsg.role.uris", new String[]{"须指定有效的模块名及其操作名，格式“模块名/操作名”，多个以英文逗号分隔。",
                    "en:A valid module name and its operation name must be specified, in the format \"module name/operation name\", and multiple names are separated by commas."});
        }
    }

    @ModelField(
            required = true, unique = true, showToList = true,
            nameI18n = {"名称", "en:Name"},
            infoI18n = {"该角色的唯一标识。", "en:Unique identifier of the role."})
    public String id;

    @ModelField(showToList = true,
            nameI18n = {"描述", "en:Description"},
            infoI18n = {"该角色的描述说明信息。", "en:Description of the role."})
    public String info = "";

    @ModelField(
            type = FieldType.checkbox,
            refModel = AppVersion.class,
            required = true,
            showToList = true,
            nameI18n = {"产品", "en:Product"},
            infoI18n = {"该角色的可以访问的产品。", "en:The product that the role can access."})
    public String app;

    @ModelField(
            type = FieldType.checkbox,
            refModel = Node.class,
            showToList = true,
            nameI18n = {"实例", "en:Instance"},
            infoI18n = {"该角色的可以访问的实例。", "en:An accessible instance of the role."})
    public String node;

    @ModelField(
            type = FieldType.checkbox,
            refModel = Cluster.class,
            showToList = true,
            nameI18n = {"集群", "en:Cluster"},
            infoI18n = {"该角色的可以访问的实例集群。", "en:The cluster of instances that the role can access."})
    public String cluster;

    @ModelField(type = FieldType.groupedMultiselect,
            nameI18n = {"权限", "en:Permissions"},
            infoI18n = {"角色的权限表示具有该角色的用户可以访问的资源（URI）集合。",
                    "en:The permissions of a role represent a collection of resources (URI) that users with that role can access."})
    public String uris;

    @ModelField(type = FieldType.bool, showToList = true,
            effectiveOnEdit = false,
            effectiveOnCreate = false,
            nameI18n = {"内置角色", "en:Built In"},
            infoI18n = {"是否是内置角色，内置角色为三员安全模型设定的角色，包括：系统管理员、安全保密管理员、安全审计员。",
                    "en:Whether it is a built-in role, the built-in role is the role set by the three-member security model, including: system administrator, security and confidentiality administrator, and security auditor."})
    public Boolean builtIn = false;

    @ModelField(type = FieldType.bool, showToList = true,
            nameI18n = {"是否激活", "en:Is Active"},
            infoI18n = {"是否激活该角色，激活后，拥有该角色的用户将可以访问该角色对应的资源，否则用户将无法访问对应的资源。",
                    "en:Whether to activate the role, after activation, users with this role will be able to access the resources corresponding to the role, otherwise the users will not be able to access the corresponding resources."})
    public Boolean active = true;

    protected String tag() {
        return "role";
    }

    @Override
    public OptionManager fieldOptions(Request request, String fieldName) {
        if ("uris".equals(fieldName)) {
            // 功能已经被禁用了
            return availableModelActions(this::isFeatureDisabled);
        }

        return super.fieldOptions(request, fieldName);
    }

    private boolean isFeatureDisabled(String appName, String model, String action) {
        String modelAction = appName + "/" + model + "/" + action;
        String disabled = ServerXml.get().getConsoleDisabled();
        if (StringUtil.isBlank(disabled)) { // 为空表示不禁用
            return false;
        }

        Set<String> disabledActions = new HashSet<>(Arrays.asList(disabled.split(Constants.DATA_SEPARATOR)));
        if (disabledActions.contains(modelAction)) {
            return true;
        }

        // 可能被级联权限禁用了，edit和list 在 Role.cascadeActions 里是 show 的二级权限，需要级联删除
        if (disabledActions.contains(appName + "/" + model + "/" + ShowModel.ACTION_NAME_SHOW)) {
            if (action.equals(EditModel.ACTION_NAME_EDIT)
                    || action.equals(ListModel.ACTION_NAME_LIST)
            ) {
                return true;
            }
        }

        // 特殊处理
        if (Constants.MODEL_NAME_encryptor.equals(model)) {
            if (disabledActions.contains(appName + "/" + model + "/" + EditModel.ACTION_NAME_UPDATE)) {
                if (action.equals(EditModel.ACTION_NAME_EDIT)) {
                    return true;
                }
            }
        }
        if (Constants.MODEL_NAME_classloaded.equals(model)) {
            if (disabledActions.contains(appName + "/" + model + "/find")) {
                return action.equals(EditModel.ACTION_NAME_EDIT);
            }
        }

        return false;
    }

    @Override
    public void add(Request request, Response response) throws Exception {
        writeForbid(request, response);
        if (!response.isSuccess()) {
            return;
        }

        Map<String, String> properties = prepareParameters(request);
        String expression = ServerXml.getTenantRoleNodeExpression(ServerXml.getTenant(request.getUserName()), null);
        XmlUtil xmlUtil = new XmlUtil(ConsoleWarHelper.getServerXml());
        xmlUtil.addNew(expression, Constants.MODEL_NAME_role, properties);
        xmlUtil.write();
    }

    @Override
    public void update(Request request, Response response) throws Exception {
        writeForbid(request, response);
        if (!response.isSuccess()) {
            return;
        }
        String tenant = ServerXml.getTenant(request.getUserName());
        String role = request.getId();
        String expression = ServerXml.getTenantRoleNodeExpression(tenant, role);
        XmlUtil xmlUtil = new XmlUtil(ConsoleWarHelper.getServerXml());
        Map<String, String> oldPro = new XmlUtil(ConsoleWarHelper.getServerXml()).getAttributes(expression);
        Map<String, String> properties = prepareParameters(request);
        xmlUtil.setAttributes(expression, properties);
        xmlUtil.write();

        Map<String, String> newPro = xmlUtil.getAttributes(expression);

        if (!ObjectUtil.isSameMap(oldPro, newPro)) {
            //RoleCache.clearRoleCache(tenant, role);
        }
    }

    @Override
    @ModelAction(name = DeleteModel.ACTION_NAME_DELETE, icon = "trash",
            effectiveWhen = "id!=root&id!=system&id!=tenant&id!=auditor",
            nameI18n = {"删除", "en:Delete"},
            infoI18n = {"删除这个组件，该组件引用的其它组件不会被删除。注：请谨慎操作，删除后不可恢复。",
                    "en:Delete this component, other components referenced by this component will not be deleted. Note: Please operate with caution, it cannot be recovered after deletion."})
    public void delete(Request request, Response response) throws Exception {
        writeForbid(request, response);
        if (!response.isSuccess()) {
            return;
        }

        XmlUtil xmlUtil = new XmlUtil(ConsoleWarHelper.getServerXml());
        xmlUtil.delete(ServerXml.getTenantRoleNodeExpression(ServerXml.getTenant(request.getUserName()), request.getId()));
        xmlUtil.write();
    }

    @Override
    public void list(Request request, Response response) throws Exception {
        // 校验权限，登录用户是否为管理员。
        if (!ServerXml.ConsoleRole.checkLoginUserIsManagerRole(request.getUserName(), true)) {
            response.setSuccess(false);
            response.setMsg(getConsoleContext().getI18N("user.not.permission"));
            return;
        }
        AddModel.super.list(request, response);
    }

    @Override
    public List<Map<String, String>> listInternal(Request request, int start, int size) throws Exception {
        List<Map<String, String>> results = new ArrayList<>();
        ServerXml.ConsoleRole.BuiltinRoleEnum[] systemRoles = ServerXml.ConsoleRole.BuiltinRoleEnum.values();
        if (start < systemRoles.length) {
            int end = Integer.min(systemRoles.length, start + size);
            for (int i = start; i < end; i++) {
                Role role = new Role();
                role.id = systemRoles[i].name();
                role.info = systemRoles[i].getInfo();
                role.builtIn = true;
                results.add(mapper(role));
            }
        }
        if (results.size() < size) {
            XmlUtil xmlUtil = new XmlUtil(ConsoleWarHelper.getServerXml());
            List<String> tenantRoleIds = xmlUtil.getAttributeList(getAllRoleIdExpression(request.getUserName()));
            int tenantRoleSize = tenantRoleIds == null ? 0 : tenantRoleIds.size();
            String tenant = ServerXml.getTenant(request.getUserName());
            start = Math.max(start - systemRoles.length, 0);
            int end = Math.min(tenantRoleSize, start + size - results.size());
            for (int i = start; i < end; i++) {
                Map<String, String> data = xmlUtil.getAttributes(ServerXml.getTenantRoleNodeExpression(tenant, tenantRoleIds.get(i)));
                switchLanguage(data);
                results.add(data);
            }
        }
        return results;
    }

    @Override
    public int getTotalSize(Request request) throws Exception {
        XmlUtil xmlUtil = new XmlUtil(ConsoleWarHelper.getServerXml());
        List<String> tenantRoleIds = xmlUtil.getAttributeList(getAllRoleIdExpression(request.getUserName()));
        int systemRoleSize = ServerXml.ConsoleRole.BuiltinRoleEnum.values().length;
        int tenantRoleSize = tenantRoleIds == null ? 0 : tenantRoleIds.size();
        return systemRoleSize + tenantRoleSize;
    }

    // 默认的中文数据，可以简单支持国际化，一旦修改了就不再支持了
    private void switchLanguage(Map<String, String> model) {
        String msg = getConsoleContext().getI18N(model.get("info"));
        if (StringUtil.notBlank(msg)) {
            model.put("info", msg);
        }
    }

    @Override
    public void show(Request request, Response response) throws Exception {
        // 校验权限，登录用户是否为管理员。
        String loginUser = request.getUserName();
        if (!ServerXml.ConsoleRole.checkLoginUserIsManagerRole(loginUser, true)) {
            response.setSuccess(false);
            response.setMsg(getConsoleContext().getI18N("user.not.permission"));
            return;
        }
        String roleId = request.getId();
        String expression = ServerXml.getTenantRoleNodeExpression(ServerXml.getTenant(loginUser), roleId);
        XmlUtil xmlUtil = new XmlUtil(ConsoleWarHelper.getServerXml());
        Map<String, String> role = xmlUtil.getAttributes(expression);
        if (role == null || role.isEmpty()) {
            for (ServerXml.ConsoleRole.BuiltinRoleEnum xRole : ServerXml.ConsoleRole.BuiltinRoleEnum.values()) {
                if (xRole.name().equals(roleId)) {
                    addRole(response, xRole);
                    break;
                }
            }
        } else {
            response.addData(role);
        }
    }

    private void addRole(Response response, ServerXml.ConsoleRole.BuiltinRoleEnum systemRole) throws Exception {
        Role role = new Role();
        role.id = systemRole.name();
        role.info = systemRole.getInfo();
        role.builtIn = true;
        response.addDataObject(role);
    }

    private void writeForbid(Request request, Response response) {
        // 校验权限，登录用户是否为管理员。
        if (!ServerXml.ConsoleRole.checkLoginUserIsManagerRole(request.getUserName(), true)) {
            response.setSuccess(false);
            response.setMsg(getConsoleContext().getI18N("user.not.permission"));
            return;
        }

        String id = request.getId();
        if (StringUtil.notBlank(id)) {
            for (ServerXml.ConsoleRole.BuiltinRoleEnum value : ServerXml.ConsoleRole.BuiltinRoleEnum.values()) {
                if (value.name().equals(id)) {
                    response.setSuccess(false);
                    response.setMsg(getConsoleContext().getI18N(qingzhou.framework.api.Constants.MASTER_APP_NAME, "operate.system.roles.not"));
                    return;
                }
            }
        }
    }

    private OptionManager availableModelActions(DisabledChecker checker) {
        List<Option> options = new ArrayList<>();
        Map<String, Option> optionCache = new HashMap<>();
        OUT:
        for (String uri : getAllUris()) {
            int i = uri.indexOf("/");
            if (i == 0) {
                throw ExceptionUtil.unexpectedException("Permission uri should contain '/': " + uri);
            } else { // IS: rest model/action
                for (String openedAction : ServerXml.ConsoleRole.openedModelActions) {
                    if (uri.equals(openedAction)) {
                        continue OUT; // 无需体现
                    }
                }

                String[] uris = uri.split("/");
                String appName = uris[0];
                String model = uris[1];
                String action = uris[2];

                ConsoleContext consoleContext = ConsoleWarHelper.getAppContext(appName).getConsoleContext();
                ModelManager modelManager = consoleContext.getModelManager();
                if (modelManager != null) {
                    boolean anyMatch = cascadeActions.values().stream().anyMatch(actionNames -> Arrays.asList(actionNames).contains(action));
                    if (anyMatch) { // 如:// 通过 ActionName.downloadfile 来级联赋权
                        continue;
                    }

                    if (ServerXml.ConsoleRole.isAuditorModel(model)) {
                        continue; // 不能包含审计员的角色
                    }

                    if (checker != null && checker.isDisabled(appName, model, action)) {
                        continue;
                    }
                    String appModel = appName + Constants.GROUP_SEPARATOR + model;
                    String[] modelI18n = modelManager.getModel(model).nameI18n();
                    String[] appModelI18n = new String[]{appName + Constants.GROUP_SEPARATOR + modelI18n[0], "en:" + appModel};
                    Option modelOption = Option.of(appModel, appModelI18n);
                    Option actionOption = Option.of(uri, modelManager.getModelAction(model, action).nameI18n());
                    optionCache.putIfAbsent(appModel, modelOption);
                    options.add(actionOption);
                }
            }
        }
        // 一二级
        options.addAll(optionCache.values());

        return () -> options;
    }

    public List<String> getAllUris() {
        List<String> allUris = new ArrayList<>();
        List<String> allAppNames = new ArrayList<>(Controller.appManager.getApps());
        if (!allAppNames.contains(qingzhou.framework.api.Constants.MASTER_APP_NAME)) {// 集中管理
            allAppNames.add(qingzhou.framework.api.Constants.MASTER_APP_NAME);
        }
        for (String appName : allAppNames) {
            ModelManager modelManager;
            try {
                modelManager = ConsoleWarHelper.getAppModelManager(appName);
                OUT:
                for (String model : modelManager.getAllModelNames()) {
                    if (qingzhou.framework.api.Constants.MASTER_APP_NAME.equals(appName)) {
                        for (String t : ServerXml.ConsoleRole.commonAppModels) {
                            if (t.equals(appName + Constants.GROUP_SEPARATOR + model)) {
                                continue OUT;
                            }
                        }
                    }
                    for (ModelAction action : modelManager.getModelActions(model)) {
                        String target = appName + "/" + model + "/" + action.name();
                        boolean isCommon = false;
                        if (qingzhou.framework.api.Constants.MASTER_APP_NAME.equals(appName)) {
                            for (String commonAction : commonActions) {
                                if (target.equals(commonAction)) {
                                    isCommon = true;
                                    break;
                                }
                            }
                        }
                        if (!isCommon) {
                            allUris.add(target);
                        }
                    }
                }
            } catch (Throwable e) {
                // 编译生成文档时候为null
            }
        }

        return allUris;
    }

    interface DisabledChecker {
        boolean isDisabled(String appName, String model, String action);
    }

    public static String getAllRoleIdExpression(String loginUser) {
        return ServerXml.getTenantRoleNodeExpression(ServerXml.getTenant(loginUser), null) + "/role/@" + ListModel.FIELD_NAME_ID;
    }
}