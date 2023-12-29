package qingzhou.console.master.security;

import qingzhou.api.console.ConsoleContext;
import qingzhou.api.console.DataStore;
import qingzhou.api.console.FieldType;
import qingzhou.api.console.Model;
import qingzhou.api.console.ModelField;
import qingzhou.api.console.data.Request;
import qingzhou.api.console.data.Response;
import qingzhou.api.console.group.Group;
import qingzhou.api.console.model.AddModel;
import qingzhou.api.console.model.ListModel;
import qingzhou.console.ServerXml;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.console.master.MasterModelBase;
import qingzhou.console.master.service.Node;
import qingzhou.console.sec.Digest;
import qingzhou.console.util.Constants;
import qingzhou.console.util.StringUtil;
import qingzhou.console.util.XmlUtil;
import qingzhou.framework.impl.ServerUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Model(name = Constants.MODEL_NAME_tenant, icon = "th",
        menuName = "Security", menuOrder = 1,
        nameI18n = {"租户管理", "en:Tenant Management"},
        infoI18n = {"租户管理。", "en:Tenant Management."})
public class Tenant extends MasterModelBase implements AddModel {
    static {
        ConsoleContext master = ConsoleWarHelper.getMasterAppConsoleContext();
        if (master != null) {
            master.addI18N("operate.system.tenants.not", new String[]{"为安全起见，请勿操作系统内置租户", "en:For security reasons, do not operate the system built-in tenants"});
        }
    }

    @ModelField(
            group = Group.GROUP_NAME_BASIC,
            required = true, unique = true, showToList = true,
            effectiveOnEdit = false,
            nameI18n = {"租户名称", "en:Tenant Name"},
            infoI18n = {"唯一标识。", "en:Unique identifier."})
    public String id;

    @ModelField(
            group = Group.GROUP_NAME_BASIC,
            required = true, showToList = true,
            noSupportZHChar = true,
            effectiveOnEdit = false,
            nameI18n = {"用户名", "en:Username"},
            infoI18n = {"租户管理员登录用户名。", "en:The tenant administrator login username."})
    public String loginName;

    @ModelField(
            group = Group.GROUP_NAME_BASIC,
            required = true,
            type = FieldType.password,
            effectiveOnEdit = false,
            nameI18n = {"密码", "en:Password"},
            infoI18n = {"租户管理员密码。", "en:Tenant admin password."})
    public String password;

    @ModelField(
            group = Group.GROUP_NAME_BASIC,
            required = true,
            type = FieldType.password,
            effectiveOnEdit = false,
            nameI18n = {"确认密码", "en:Confirm Password"},
            infoI18n = {"确认租户管理员登录系统的新密码。", "en:Confirm the tenant administrator new password to log in to the system."})
    public String confirmPassword;

    @ModelField(
            group = Group.GROUP_NAME_BASIC,
            type = FieldType.multiselect,
            showToList = true,
            refModel = Node.class,
            nameI18n = {"节点", "en:Node"},
            infoI18n = {"租户可使用的节点资源。", "en:Node resources that are available to tenants."})
    public String node;

    @ModelField(
            group = Group.GROUP_NAME_BASIC,
            showToList = true,
            nameI18n = {"描述", "en:Description"},
            infoI18n = {"描述信息。", "en:Description information."})
    public String info = "";

    @Override
    public String validate(Request request, String fieldName) {
        if (User.pwdKey.equals(fieldName)) {
            String newValue = request.getParameter(User.pwdKey);
            String userName = request.getParameter("loginName");
            String msg = User.checkPwd(newValue, userName);
            if (msg != null) {
                return msg;
            }
        }

        if (User.confirmPwdKey.equals(fieldName)) {
            String newValue = request.getParameter(User.confirmPwdKey);
            String password = request.getParameter(User.pwdKey);
            // 恢复 ITAIT-5005 的修改
            if (!Objects.equals(password, newValue)) {
                return getConsoleContext().getI18N("confirmPassword.different");
            }
        }

        return super.validate(request, fieldName);
    }

    @Override
    public void add(Request request, Response response) throws Exception {
        // 校验用户是否为超级管理员，系统管理员
        String loginUser = request.getUserName();
        if(!ServerXml.ConsoleRole.checkLoginUserIsManagerRole(loginUser, false)){
            response.setSuccess(false);
            response.setMsg(getConsoleContext().getI18N("user.not.permission"));
            return;
        }

        Map<String, String> properties = prepareParameters(request);
        String tenant = properties.get("id");
        String loginName = properties.remove("loginName");
        String password = properties.remove(User.pwdKey);
        properties.remove(User.confirmPwdKey);
        XmlUtil xmlUtils = new XmlUtil(ServerUtil.getServerXml());
        xmlUtils.addNew("//root/console/auth/tenants", "tenant", properties);

        Map<String, String> userMap = new HashMap<>();
        userMap.put("id", loginName);
        userMap.put(User.pwdKey, password);
        buildTenantAdminUser(userMap);
        if (!xmlUtils.isNodeExists(ServerXml.getTenantUserNodeExpression(tenant, null))) {
            xmlUtils.addNew("//root/console/auth/tenants/tenant[@" + ListModel.FIELD_NAME_ID + "='" + tenant + "']", "users", null);
        }
        xmlUtils.addNew(ServerXml.getTenantUserNodeExpression(tenant, null), "user", userMap);
        xmlUtils.write();
    }

    @Override
    public void delete(Request request, Response response) throws Exception {
        String id = request.getId();
        if (StringUtil.notBlank(id) && "default".equals(id)) {
            response.setSuccess(false);
            response.setMsg(getConsoleContext().getI18N("operate.system.tenants.not"));
            return;
        }
        DataStore dataStore = getDataStore();
        dataStore.deleteDataById(request.getModelName(), request.getId());
    }

    private void buildTenantAdminUser(Map<String, String> userMap) {
        String password = userMap.remove(User.pwdKey);
        userMap.put(User.pwdKey, Digest.mutate(password,
                "SHA-256",
                User.defSaltLength,
                User.defIterations));
        userMap.put("active", "true");
        userMap.put("enable2FA", "false");
        userMap.put("passwordMinAge", "0");
        userMap.put("passwordMaxAge", "90");
        userMap.put("enablePasswordAge", "true");
        userMap.put("changeInitPwd", "true");
        userMap.put("iterations", String.valueOf(User.defIterations));
        userMap.put("saltLength", String.valueOf(User.defSaltLength));
        userMap.put("digestAlg", "SHA-256");
        userMap.put("roles", ServerXml.ConsoleRole.BuiltinRoleEnum.tenant.name());
        userMap.put("info", ServerXml.ConsoleRole.BuiltinRoleEnum.tenant.getInfo());
    }
}
