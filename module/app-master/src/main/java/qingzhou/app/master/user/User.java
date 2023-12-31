package qingzhou.app.master.user;

import qingzhou.app.master.MasterModelBase;
import qingzhou.framework.api.AddModel;
import qingzhou.framework.api.Model;

@Model(name = "user", icon = "user",
        menuName = "Security", menuOrder = 1,
        nameI18n = {"管理员", "en:User"},
        infoI18n = {"管理登录和操作服务器的管理员，管理员可登录控制台、REST接口等。", "en:Manages the administrator who logs in and operates the server. The administrator can log in to the console, REST interface, etc."})
public class User extends MasterModelBase implements AddModel {
    public static final String pwdKey = "password";
    public static final String confirmPwdKey = "confirmPassword";
    public static final int defSaltLength = 4;
    public static final int defIterations = 5;
    public static final int defLimitRepeats = 5;
//
//    static {
//        ConsoleContext master = ServerUtil.getMasterConsoleContext();
//        master.addI18N("confirmPassword.different", new String[]{"输入的确认密码与密码不一致", "en:Confirm that the password does not match the new password"});
//        master.addI18N("permissions.cannot.users", new String[]{"出于安全考虑，系统内置用户的权限不能更改", "en:For security reasons, permissions cannot be changed for built-in users of the system"});
//        master.addI18N("System.users.keep.active", new String[]{"系统内置用户需要保持启用", "en:System built-in users need to keep active"});
//        master.addI18N("operate.system.users.not", new String[]{"为安全起见，请勿操作系统内置用户", "en:For security reasons, do not operate the system built-in users"});
//        master.addI18N("tenant.not.exist", new String[]{"租户[%s]不存在", "en:Tenant [%s] does not exist"});
//
//    }
//
//    @ModelField(
//            group = Group.GROUP_NAME_BASIC,
//            required = true, unique = true, showToList = true,
//            nameI18n = {"名称", "en:Name"},
//            infoI18n = {"唯一标识。", "en:Unique identifier."})
//    public String id;
//
//    @ModelField(
//            group = Group.GROUP_NAME_BASIC,
//            showToList = true,
//            nameI18n = {"描述", "en:Description"},
//            infoI18n = {"描述信息。", "en:Description information."})
//    public String info = "";
//
//    @ModelField(
//            group = Group.GROUP_NAME_BASIC,
//            showToList = true,
//            effectiveOnEdit = false,
//            effectiveOnCreate = false,
//            nameI18n = {"所属租户", "en:Tenant"},
//            infoI18n = {"所属租户。", "en:Tenant to which it belongs."})
//    public String tenant = "";
//
//    @ModelField(
//            group = Group.GROUP_NAME_BASIC,
//            required = true,
//            type = FieldType.password,
//            nameI18n = {"密码", "en:Password"},
//            infoI18n = {"密码", "en:Password"})
//    public String password;
//
//    @ModelField(
//            group = Group.GROUP_NAME_BASIC,
//            required = true,
//            type = FieldType.password,
//            nameI18n = {"确认密码", "en:Confirm Password"},
//            infoI18n = {"确认管理员登录系统的新密码。", "en:Confirm the administrator new password for logging in to the system."})
//    public String confirmPassword;
//
//    @ModelField(
//            group = Group.GROUP_NAME_BASIC,
//            required = true,
//            type = FieldType.select,
//            nameI18n = {"摘要算法", "en:Digest Algorithm"},
//            infoI18n = {"进行摘要加密所采用的算法。", "en:The algorithm used for digest encryption."}
//    )
//    public String digestAlg = "SHA-256";
//
//    @ModelField(
//            group = Group.GROUP_NAME_BASIC,
//            type = FieldType.number,
//            min = 1,
//            max = 128,
//            nameI18n = {"加盐长度", "en:Salt Length"},
//            infoI18n = {"将自动生成的盐值和字符串一起加密可以提高加密强度。", "en:Encrypting the automatically generated salt value along with the string increases the encryption strength."}
//    )
//    public Integer saltLength = defSaltLength;
//
//    @ModelField(
//            group = Group.GROUP_NAME_BASIC,
//            type = FieldType.number,
//            min = 1,
//            max = 128,
//            nameI18n = {"迭代次数", "en:Iterations"},
//            infoI18n = {"连续多次摘要加密可以提高加密强度。", "en:The encryption strength can be improved by multiple digest encryption."}
//    )
//    public Integer iterations = defIterations;
//
//    @ModelField(
//            group = "security",
//            type = FieldType.bool,
//            nameI18n = {"须修改初始密码", "en:Change Initial Password"},
//            infoI18n = {"安全起见，开启该功能后，初始密码须修改以后才能登录系统。",
//                    "en:For security reasons, the initial password must be changed before you can log in to the system once this function is enabled."})
//    public Boolean changeInitPwd = true;
//
//    @ModelField(
//            group = "security",
//            type = FieldType.bool,
//            nameI18n = {"密码期限", "en:Password Age"},
//            infoI18n = {"开启该功能，可限制密码的使用期限。",// 内部：0 表示可以永久不更新。
//                    "en:Enable this feature to limit the expiration date of the password."}
//    )
//    public Boolean enablePasswordAge = true;
//
//    @ModelField(
//            group = "security",
//            effectiveWhen = "enablePasswordAge=true",
//            type = FieldType.number,
//            min = 1, max = 90,
//            noLessThan = "passwordMinAge",
//            nameI18n = {"密码最长使用期限", "en:Maximum Password Age"},
//            infoI18n = {"用户登录系统的密码距离上次修改超过该期限（单位为天）后，需首先更新密码才能继续登录系统。",// 内部：0 表示可以永久不更新。
//                    "en:After the password of the user logging in to the system has been last modified beyond this period (in days), the user must first update the password before continuing to log in to the system."}
//    )
//    public Integer passwordMaxAge = 90;
//
//    @ModelField(
//            group = "security",
//            effectiveWhen = "enablePasswordAge=true",
//            type = FieldType.number,
//            min = 0,
//            noGreaterThan = "passwordMaxAge",
//            nameI18n = {"密码最短使用期限", "en:Minimum Password Age"},
//            infoI18n = {"用户登录系统的密码距离上次修改未达到该期限（单位为天），则不能进行更新。0 表示可以随时更新。",
//                    "en:If the user password for logging in to the system has not reached this period (in days) since the last modification, it cannot be updated. 0 means that it can be updated at any time."}
//    )
//    public Integer passwordMinAge = 0;
//
//    @ModelField(
//            group = "security",
//            type = FieldType.bool,
//            showToList = true,
//            nameI18n = {"双因子认证", "en:Two-factor Authentication"},
//            infoI18n = {"用户开启双因子认证后，在登录系统时，除验证用户的登录密码之外，还会验证用户的动态密码（由用户终端的双因子认证客户端设备产生），全部验证通过后才允许登录系统。开启双因子认证会自动初始化密钥，该密钥需要在用户的双因子认证客户端上同步绑定（通常是用户通过手机扫描密码修改页面的二维码来进行）。",
//                    "en:After a user enables two-factor authentication, when logging into the system, in addition to verifying the user login password, the user dynamic password (generated by the two-factor authentication client device of the user terminal) will also be verified, and the system will be allowed to log in after all verifications are passed. . Enabling two-factor authentication will automatically initialize the key, which needs to be synchronously bound on the user two-factor authentication client (usually the user scans the QR code on the password modification page through the mobile phone)."}
//    )
//    public Boolean enable2FA = false;
//
//    @ModelField(
//            group = "security",
//            type = FieldType.bool, showToList = true, nameI18n = {"是否激活", "en:Is Active"}, infoI18n = {"若未激活，则无法登录服务器。", "en:If it is not activated, you cannot log in to the server."})
//    public Boolean active = true;
//
//    @ModelField(
//            group = "security",
//            showToList = true,
//            effectiveOnCreate = false, effectiveOnEdit = false,
//            nameI18n = {"密码最后修改时间", "en:Password Last Modified"},
//            infoI18n = {"最后一次修改密码的日期和时间。", "en:The date the password was last changed."}
//    )
//    public String passwordLastModifiedTime;
//
//    @ModelField(
//            group = "security",
//            type = FieldType.number, min = 1, max = 10,
//            nameI18n = {"不与最近密码重复", "en:Recent Password Restrictions"},
//            infoI18n = {"限制本次更新的密码不能和最近几次使用过的密码重复。注：设置为 “1” 表示只要不与当前密码重复即可。",
//                    "en:Restrict this update password to not be duplicated by the last few times you have used. Note: A setting of 1 means as long as it does not duplicate the current password."})
//    public Integer limitRepeats = defLimitRepeats;
//
//    @ModelField(
//            group = "security",
//            showToEdit = false,
//            showToShow = false,
//            effectiveOnCreate = false,
//            nameI18n = {"历史密码", "en:Historical Passwords"},
//            infoI18n = {"记录最近几次使用过的密码。", "en:Keep a record of the last few passwords you have used."})
//    public String oldPasswords;
//
//    @Override
//    public GroupManager fieldGroups(String groupName) {
//        if (groupName.equals("security")) {
//            return () -> Collections.singletonList(Group.of("security", new String[]{"安全", "en:Security"}));
//        }
//
//        return super.fieldGroups(groupName);
//    }
//
//    @Override
//    public OptionManager fieldOptions(Request request, String fieldName) {
//        if ("digestAlg".equals(fieldName)) {
//            return () -> digestAlgFieldOptions();
//        }
//
//        return super.fieldOptions(request, fieldName);
//    }
//
//    public List<Option> digestAlgFieldOptions() {
//        return new ArrayList<>(Arrays.asList(Option.of("SHA-256", new String[]{"SHA-256", "en:SHA-256"}),
//                Option.of("SHA-384", new String[]{"SHA-384", "en:SHA-384"}),
//                Option.of("SHA-512", new String[]{"SHA-512", "en:SHA-512"})));
//    }
//
//    public void list(Request request, Response response) throws Exception {
//        String loginUser = request.getUserName();
//        if (!ServerXml.ConsoleRole.checkLoginUserIsManagerRole(loginUser, true)) {
//            response.setSuccess(false);
//            response.setMsg(getConsoleContext().getI18N("user.not.permission"));
//            return;
//        }
//
//        if (ServerXml.ConsoleRole.isRootUser(loginUser)) {
//            // TODO 超级管理员能看所有
//        }
//        AddModel.super.list(request, response);
//    }
//
//    @Override
//    public List<Map<String, String>> listInternal(Request request, int start, int size) throws Exception {
//        XmlUtil xmlUtil = new XmlUtil(ConsoleWarHelper.getServerXml());
//        List<String> tenantUserIds = xmlUtil.getAttributeList(getAllUserIdExpression(request.getUserName()));
//        String tenant = ServerXml.getTenant(request.getUserName());
//        List<Map<String, String>> results = new ArrayList<>();
//        for (int i = start; i < Integer.min(start + size, tenantUserIds.size()); i++) {
//            Map<String, String> user = xmlUtil.getAttributes(ServerXml.getTenantUserNodeExpression(tenant, tenantUserIds.get(i)));
//            user.put("tenant", tenant);
//            String pwd = user.get(ListModel.FIELD_NAME_ID);
//            if (pwd != null) {
//                String[] pwdParts = pwd.split("\\$");
//                if (pwdParts.length == 4) {
//                    user.put("digestAlg", pwdParts[3]);
//                    user.put("saltLength", String.valueOf(pwdParts[0].length() / 2));
//                    user.put("iterations", pwdParts[1]);
//                }
//            }
//            switchLanguage(user);
//            results.add(user);
//        }
//        return results;
//    }
//
//    @Override
//    public int getTotalSize(Request request) throws Exception {
//        XmlUtil xmlUtil = new XmlUtil(ConsoleWarHelper.getServerXml());
//        List<String> tenantUserIds = xmlUtil.getAttributeList(getAllUserIdExpression(request.getUserName()));
//        return tenantUserIds == null ? 0 : tenantUserIds.size();
//    }
//
//    // 默认的中文数据，可以简单支持国际化，一旦修改了就不再支持了
//    private void switchLanguage(Map<String, String> user) {
//        String msg = getConsoleContext().getI18N(user.get("info"));
//        if (StringUtil.notBlank(msg)) {
//            user.put("info", msg);
//        }
//    }
//
//    @Override
//    public void add(Request request, Response response) throws Exception {
//        writeForbid(request, response);
//        if (!response.isSuccess()) {
//            return;
//        }
//        String tenant = ServerXml.getTenant(request.getUserName());
//        if (StringUtil.isBlank(tenant)) {
//            response.setSuccess(false);
//            response.setMsg(getConsoleContext().getI18N("tenant.not.exist", tenant));
//            return;
//        }
//        Map<String, String> properties = prepareParameters(request);
//        rectifyParameters(properties, tenant);
//        XmlUtil xmlUtils = new XmlUtil(ConsoleWarHelper.getServerXml());
//        xmlUtils.addNew(ServerXml.getTenantUserNodeExpression(tenant, null), "user", properties);
//        xmlUtils.write();
//    }
//
//    @Override
//    public String validate(Request request, String fieldName) {
//        if (pwdKey.equals(fieldName)) {
//            String newValue = request.getParameter(pwdKey);
//            if (passwordChanged(newValue)) {
//                String userName = request.getParameter("id");
//                String msg = checkPwd(newValue, userName);
//                if (msg != null) {
//                    return msg;
//                }
//            }
//        }
//
//        if (confirmPwdKey.equals(fieldName)) {
//            String newValue = request.getParameter(confirmPwdKey);
//            String password = request.getParameter(pwdKey);
//            // 恢复 ITAIT-5005 的修改
//            if (!Objects.equals(password, newValue)) {
//                return getConsoleContext().getI18N("confirmPassword.different");
//            }
//        }
//
//        return super.validate(request, fieldName);
//    }
//
//    public static String checkPwd(String password, String... infos) {
//        if (Constants.PASSWORD_FLAG.equals(password)) {
//            return null;
//        }
//        ConsoleContext consoleContext = ServerUtil.getMasterConsoleContext();
//        int minLength = 10;
//        int maxLength = 20;
//        if (password.length() < minLength || password.length() > maxLength) {
//            return String.format(consoleContext.getI18N("validator.lengthBetween"), minLength, maxLength);
//        }
//
//        if (infos != null && infos.length > 0) {
//            if (infos[0] != null) { // for #ITAIT-5014
//                if (password.contains(infos[0])) { // 包含身份信息
//                    return consoleContext.getI18N("password.passwordContainsUsername");
//                }
//            }
//        }
//
//        //特殊符号包含下划线
//        String PASSWORD_REGEX = "^(?![A-Za-z0-9]+$)(?![a-z0-9_\\W]+$)(?![A-Za-z_\\W]+$)(?![A-Z0-9_\\W]+$)(?![A-Z0-9\\W]+$)[\\w\\W]{10,}$";
//        if (!Pattern.compile(PASSWORD_REGEX).matcher(password).matches()) {
//            return consoleContext.getI18N("password.format");
//        }
//
//        if (StringUtil.isContinuousChar(password)) { // 连续字符校验
//            return consoleContext.getI18N("password.continuousChars");
//        }
//
//        return null;
//    }
//
//
//    @Override
//    public void update(Request request, Response response) throws Exception {
//        writeForbid(request, response);
//        if (!response.isSuccess()) {
//            return;
//        }
//        String tenant = ServerXml.getTenant(request.getUserName());
//        if (StringUtil.isBlank(tenant)) {
//            response.setSuccess(false);
//            response.setMsg(getConsoleContext().getI18N("tenant.not.exist", tenant));
//            return;
//        }
//        Map<String, String> oldPro = new XmlUtil(ConsoleWarHelper.getServerXml()).getAttributes(ServerXml.getTenantUserNodeExpression(tenant, request.getId()));
//        DataStore dataStore = getDataStore();
//        Map<String, String> properties = prepareParameters(request);
//        dataStore.updateSpecifiedData(request.getModelName(), request.getId(), properties);
//
//        Map<String, String> newPro = new XmlUtil(ConsoleWarHelper.getServerXml()).getAttributes(ServerXml.getTenantUserNodeExpression(tenant, request.getId()));
//
//        // 检查是否要重新登录: 简单设计，只要更新即要求重新登录，这可用于强制踢人
//        if (!ObjectUtil.isSameMap(oldPro, newPro)) {
////            String encodeUser = LoginManager.encodeUser(actionContext.getId());
////            ActionContext.invalidateAllSessionAsAttribute(actionContext.getHttpServletRequestInternal(),
////                    LoginManager.LOGIN_USER, encodeUser);
//        }
//    }
//
//    protected Map<String, String> rectifyParameters(Map<String, String> params, String tenant) throws Exception {
//        String password = params.remove(pwdKey);
//        params.remove(confirmPwdKey);
//        boolean passwordChanged = passwordChanged(password);
//        if (passwordChanged) {
//            String digestAlg = params.get("digestAlg");
//            String saltLength = params.get("saltLength");
//            String iterations = params.get("iterations");
//            MessageDigest digest = ConsoleWarHelper.getCryptoService().getMessageDigest();
//            params.put(pwdKey, digest.digest(password,
//                    digestAlg,
//                    Integer.parseInt(saltLength),
//                    Integer.parseInt(iterations)));
//            insertPasswordModifiedTime(params);
//        }
//
//        Map<String, String> oldPro = new XmlUtil(ConsoleWarHelper.getServerXml()).getAttributes(ServerXml.getTenantUserNodeExpression(tenant, params.get("id")));
//        if (oldPro == null) {
//            oldPro = new HashMap<>();
//        }
//        String oldPasswords = oldPro.get("oldPasswords");
//        String limitRepeats = params.get("limitRepeats");
//        if (StringUtil.isBlank(limitRepeats)) {
//            limitRepeats = oldPro.get("limitRepeats");
//        }
//
//        String cutOldPasswords = cutOldPasswords(oldPasswords, limitRepeats, passwordChanged ? params.get(pwdKey) : null);
//        params.put("oldPasswords", cutOldPasswords);
//        params.put("tenant", tenant);
//
//        return params;
//    }
//
//    public static String cutOldPasswords(String oldPasswords, String repeats, String newPwd) {
//        if (oldPasswords == null) {
//            oldPasswords = "";
//        }
//        if (StringUtil.notBlank(newPwd)) {
//            oldPasswords += (oldPasswords.isEmpty() ? "" : Constants.DATA_SEPARATOR) + newPwd;
//        }
//
//        if (StringUtil.isBlank(repeats)) {
//            repeats = String.valueOf(User.defLimitRepeats);
//        }
//        int currentLength;
//        if (!oldPasswords.contains(Constants.DATA_SEPARATOR)) {
//            currentLength = 1;
//        } else {
//            currentLength = oldPasswords.split(Constants.DATA_SEPARATOR).length;
//        }
//        int limitRepeats = Integer.parseInt(repeats);
//        int cutCount = currentLength - limitRepeats;
//        if (cutCount > 0) {
//            for (int i = 0; i < cutCount; i++) {
//                int found = oldPasswords.indexOf(Constants.DATA_SEPARATOR);
//                if (found != -1) {
//                    oldPasswords = oldPasswords.substring(found + Constants.DATA_SEPARATOR.length());
//                }
//            }
//        }
//        return oldPasswords;
//    }
//
//    private void writeForbid(Request request, Response response) {
//        String loginUser = request.getUserName();
//        if (!ServerXml.ConsoleRole.checkLoginUserIsManagerRole(loginUser, true)) {
//            response.setSuccess(false);
//            response.setMsg(getConsoleContext().getI18N("user.not.permission"));
//            return;
//        }
//
//        String id = request.getId();
//        if (StringUtil.notBlank(id)) {
//            if (ServerXml.ConsoleRole.systemXUsers().contains(id)) {
//                if (AddModel.ACTION_NAME_ADD.equals(request.getActionName())
//                        || DeleteModel.ACTION_NAME_DELETE.equals(request.getActionName())) {
//                    response.setSuccess(false);
//                    response.setMsg(getConsoleContext().getI18N("operate.system.users.not"));
//                    return;
//                }
//
//                if (EditModel.ACTION_NAME_UPDATE.equals(request.getActionName())) {
//                    if (!Boolean.parseBoolean(request.getParameter("active"))) {
//                        response.setSuccess(false);
//                        response.setMsg(getConsoleContext().getI18N("System.users.keep.active"));
//                    }
//                }
//            }
//        }
//    }
//
//    @Override
//    @ModelAction(
//            name = DeleteModel.ACTION_NAME_DELETE,
//            effectiveWhen = "id!=thanos&id!=security&id!=auditor&id!=monitor",
//            icon = "trash",
//            nameI18n = {"删除", "en:Delete"},
//            infoI18n = {"删除这个组件，该组件引用的其它组件不会被删除。注：请谨慎操作，删除后不可恢复。",
//                    "en:Delete this component, other components referenced by this component will not be deleted. Note: Please operate with caution, it cannot be recovered after deletion."})
//    public void delete(Request request, Response response) throws Exception {
//        writeForbid(request, response);
//        if (!response.isSuccess()) {
//            return;
//        }
//
//        DataStore dataStore = getDataStore();
//        dataStore.deleteDataById(request.getModelName(), request.getId());
//    }
//
//    static void insertPasswordModifiedTime(Map<String, String> params) {
//        String value = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
//        params.put("passwordLastModifiedTime", value);
//    }
//
//
//    private boolean passwordChanged(String password) {
//        return password != null && !Constants.PASSWORD_FLAG.equals(password);
//    }
//
//    static void updateXmlProperty(String userName, String key, String val) throws Exception {
//        XmlUtil xmlUtils = new XmlUtil(ConsoleWarHelper.getServerXml());
//
//        Map<String, String> p = new HashMap<>();
//        p.put(key, val);
//        String tenant = ServerXml.getTenant(userName);
//        String user = ServerXml.getLoginUserName(userName);
//        xmlUtils.setAttributes(ServerXml.getTenantUserNodeExpression(tenant, user), p);
//        xmlUtils.write();
//    }
//
//    public static String getAllUserIdExpression(String loginUser) {
//        return ServerXml.getTenantUserNodeExpression(ServerXml.getTenant(loginUser), null) + "/user/@" + ListModel.FIELD_NAME_ID;
//    }
}
