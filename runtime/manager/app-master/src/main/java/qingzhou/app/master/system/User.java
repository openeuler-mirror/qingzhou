package qingzhou.app.master.system;

import qingzhou.api.*;
import qingzhou.api.type.*;
import qingzhou.app.master.MasterApp;
import qingzhou.framework.Constants;
import qingzhou.framework.app.AppInfo;
import qingzhou.framework.crypto.CryptoService;
import qingzhou.framework.crypto.MessageDigest;
import qingzhou.framework.util.StringUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Model(name = "user", icon = "user",
        menuName = "System", menuOrder = 1,
        nameI18n = {"系统用户", "en:System User"},
        infoI18n = {"管理登录和操作服务器的用户，用户可登录控制台、REST接口等。", "en:Manages the user who logs in and operates the server. The user can log in to the console, REST interface, etc."})
public class User extends ModelBase implements Createable {
    public static final String pwdKey = "password";
    public static final String confirmPwdKey = "confirmPassword";
    public static final int defSaltLength = 4;
    public static final int defIterations = 5;
    public static final int defLimitRepeats = 5;
    public static final String DATA_SEPARATOR = ",";
    public static final String PASSWORD_FLAG = "***************";

    @Override
    public void init() {
        AppContext appContext = getAppContext();
        appContext.addI18n("confirmPassword.different", new String[]{"输入的确认密码与密码不一致", "en:Confirm that the password does not match the new password"});
        appContext.addI18n("System.users.keep.active", new String[]{"系统内置用户需要保持启用", "en:System built-in users need to keep active"});
        appContext.addI18n("operate.system.users.not", new String[]{"为安全起见，请勿操作系统内置用户", "en:For security reasons, do not operate the system built-in users"});
    }

    @ModelField(
            required = true, showToList = true,
            nameI18n = {"用户名", "en:User Name"},
            infoI18n = {"用于登录系统的用户名。", "en:The username used to log in to the system."})
    public String id;

    @ModelField(
            required = true,
            type = FieldType.password,
            nameI18n = {"密码", "en:Password"},
            infoI18n = {"密码", "en:Password"})
    public String password;

    @ModelField(
            required = true,
            type = FieldType.password,
            nameI18n = {"确认密码", "en:Confirm Password"},
            infoI18n = {"确认登录系统的新密码。", "en:Confirm the new password for logging in to the system."})
    public String confirmPassword;

    @ModelField(
            required = true,
            type = FieldType.select,
            nameI18n = {"摘要算法", "en:Digest Algorithm"},
            infoI18n = {"进行摘要加密所采用的算法。", "en:The algorithm used for digest encryption."}
    )
    public String digestAlg = "SHA-256";

    @ModelField(
            type = FieldType.number,
            min = 1,
            max = 128,
            nameI18n = {"加盐长度", "en:Salt Length"},
            infoI18n = {"将自动生成的盐值和字符串一起加密可以提高加密强度。", "en:Encrypting the automatically generated salt value along with the string increases the encryption strength."}
    )
    public Integer saltLength = defSaltLength;

    @ModelField(
            min = 1,
            max = 128,
            nameI18n = {"迭代次数", "en:Iterations"},
            infoI18n = {"连续多次摘要加密可以提高加密强度。", "en:The encryption strength can be improved by multiple digest encryption."}
    )
    public Integer iterations = defIterations;

    @ModelField(
            type = FieldType.checkbox,
            required = true,
            showToList = true,
            nameI18n = {"可用节点", "en:Available Nodes"},
            infoI18n = {"选择用户可使用的节点。", "en:Select the nodes that are available to the user."})
    public String nodes = AppInfo.SYS_NODE_LOCAL;

    @ModelField(
            group = "security",
            type = FieldType.bool,
            nameI18n = {"须修改初始密码", "en:Change Initial Password"},
            infoI18n = {"安全起见，开启该功能后，初始密码须修改以后才能登录系统。",
                    "en:For security reasons, the initial password must be changed before you can log in to the system once this function is enabled."})
    public Boolean changeInitPwd = true;

    @ModelField(
            group = "security",
            type = FieldType.bool,
            nameI18n = {"密码期限", "en:Password Age"},
            infoI18n = {"开启该功能，可限制密码的使用期限。",// 内部：0 表示可以永久不更新。
                    "en:Enable this feature to limit the expiration date of the password."}
    )
    public Boolean enablePasswordAge = true;

    @ModelField(
            group = "security",
            effectiveWhen = "enablePasswordAge=true",
            type = FieldType.number,
            min = 1, max = 90,
            noLessThan = "passwordMinAge",
            nameI18n = {"密码最长使用期限", "en:Maximum Password Age"},
            infoI18n = {"用户登录系统的密码距离上次修改超过该期限（单位为天）后，需首先更新密码才能继续登录系统。",// 内部：0 表示可以永久不更新。
                    "en:After the password of the user logging in to the system has been last modified beyond this period (in days), the user must first update the password before continuing to log in to the system."}
    )
    public Integer passwordMaxAge = 90;

    @ModelField(
            group = "security",
            effectiveWhen = "enablePasswordAge=true",
            type = FieldType.number,
            min = 0,
            noGreaterThan = "passwordMaxAge",
            nameI18n = {"密码最短使用期限", "en:Minimum Password Age"},
            infoI18n = {"用户登录系统的密码距离上次修改未达到该期限（单位为天），则不能进行更新。0 表示可以随时更新。",
                    "en:If the user password for logging in to the system has not reached this period (in days) since the last modification, it cannot be updated. 0 means that it can be updated at any time."}
    )
    public Integer passwordMinAge = 0;

    @ModelField(
            group = "security",
            type = FieldType.bool,
            showToList = true,
            nameI18n = {"双因子认证", "en:Two-factor Authentication"},
            infoI18n = {"用户开启双因子认证后，在登录系统时，除验证用户的登录密码之外，还会验证用户的动态密码（由用户终端的双因子认证客户端设备产生），全部验证通过后才允许登录系统。开启双因子认证会自动初始化密钥，该密钥需要在用户的双因子认证客户端上同步绑定（通常是用户通过手机扫描密码修改页面的二维码来进行）。",
                    "en:After a user enables two-factor authentication, when logging into the system, in addition to verifying the user login password, the user dynamic password (generated by the two-factor authentication client device of the user terminal) will also be verified, and the system will be allowed to log in after all verifications are passed. . Enabling two-factor authentication will automatically initialize the key, which needs to be synchronously bound on the user two-factor authentication client (usually the user scans the QR code on the password modification page through the mobile phone)."}
    )
    public Boolean enable2FA = false;

    @ModelField(
            group = "security",
            type = FieldType.bool, showToList = true, nameI18n = {"是否激活", "en:Is Active"}, infoI18n = {"若未激活，则无法登录服务器。", "en:If it is not activated, you cannot log in to the server."})
    public Boolean active = true;

    @ModelField(
            group = "security",
            showToList = true,
            disableOnCreate = true, disableOnEdit = true,
            nameI18n = {"密码最后修改时间", "en:Password Last Modified"},
            infoI18n = {"最后一次修改密码的日期和时间。", "en:The date the password was last changed."}
    )
    public String passwordLastModifiedTime;

    @ModelField(
            group = "security",
            type = FieldType.number, min = 1, max = 10,
            nameI18n = {"不与最近密码重复", "en:Recent Password Restrictions"},
            infoI18n = {"限制本次更新的密码不能和最近几次使用过的密码重复。注：设置为 “1” 表示只要不与当前密码重复即可。",
                    "en:Restrict this update password to not be duplicated by the last few times you have used. Note: A setting of 1 means as long as it does not duplicate the current password."})
    public Integer limitRepeats = defLimitRepeats;

    @ModelField(
            group = "security",
            showToEdit = false,
            disableOnCreate = true,
            nameI18n = {"历史密码", "en:Historical Passwords"},
            infoI18n = {"记录最近几次使用过的密码。", "en:Keep a record of the last few passwords you have used."})
    public String oldPasswords;

    @Override
    public Groups groups() {
        return Groups.of(Group.of("security", new String[]{"安全", "en:Security"}));
    }

    @Override
    public Options options(Request request, String fieldName) {
        if ("digestAlg".equals(fieldName)) {
            return Options.of("SHA-256", "SHA-384", "SHA-512");
        }

        return super.options(request, fieldName);
    }


    @ModelAction(name = Createable.ACTION_NAME_ADD,
            icon = "save",
            nameI18n = {"添加", "en:Add"},
            infoI18n = {"按配置要求创建一个模块。", "en:Create a module as configured."})
    public void add(Request request, Response response) throws Exception {
        if (!checkForbidden(request, response)) {
            return;
        }
        Map<String, String> newUser = MasterApp.prepareParameters(request, getAppContext());
        rectifyParameters(request, newUser, new HashMap<>());
        getDataStore().addData(request.getModelName(), newUser.get(Listable.FIELD_NAME_ID), newUser);
    }


    @ModelAction(name = Showable.ACTION_NAME_SHOW,
            icon = "info-sign", forwardToPage = "show",
            nameI18n = {"查看", "en:Show"},
            infoI18n = {"查看该组件的相关信息。", "en:View the information of this model."})
    public void show(Request request, Response response) throws Exception {
        DataStore dataStore = getDataStore();
        Map<String, String> data = dataStore.getDataById(request.getModelName(), request.getId());
        if (Editable.ACTION_NAME_EDIT.equals(request.getActionName())) {
            data.put("password", PASSWORD_FLAG);
            data.put("confirmPassword", PASSWORD_FLAG);
        }
        response.addData(data);
    }

    @Override
    public String validate(Request request, String fieldName) {
        if (pwdKey.equals(fieldName)) {
            String newValue = request.getParameter(pwdKey);
            if (passwordChanged(newValue)) {
                String userName = request.getParameter(Listable.FIELD_NAME_ID);
                String msg = checkPwd(request, newValue, userName);
                if (msg != null) {
                    return msg;
                }
            }
        }

        if (confirmPwdKey.equals(fieldName)) {
            String newValue = request.getParameter(confirmPwdKey);
            String password = request.getParameter(pwdKey);
            // 恢复 ITAIT-5005 的修改
            if (!Objects.equals(password, newValue)) {
                return getAppContext().getAppMetadata().getI18n(request.getI18nLang(), "confirmPassword.different");
            }
        }

        return null;
    }


    @ModelAction(name = Editable.ACTION_NAME_UPDATE,
            icon = "save",
            nameI18n = {"更新", "en:Update"},
            infoI18n = {"更新这个模块的配置信息。", "en:Update the configuration information for this module."})
    public void update(Request request, Response response) throws Exception {
        if (!checkForbidden(request, response)) {
            return;
        }

        DataStore dataStore = getDataStore();
        String modelName = request.getModelName();
        String userId = request.getId();
        Map<String, String> oldUser = dataStore.getDataById(modelName, userId);
        Map<String, String> newUser = MasterApp.prepareParameters(request, getAppContext());
        rectifyParameters(request, newUser, oldUser);
        dataStore.updateDataById(modelName, userId, newUser);

        Map<String, String> newPro = dataStore.getDataById(modelName, userId);

        // 检查是否要重新登录: 简单设计，只要更新即要求重新登录，这可用于强制踢人
        if (!isSameMap(oldUser, newPro)) {
//            String encodeUser = LoginManager.encodeUser(actionContext.getId());
//            ActionContext.invalidateAllSessionAsAttribute(actionContext.getHttpServletRequestInternal(),
//                    LoginManager.LOGIN_USER, encodeUser);
        }
    }

    public static boolean isSameMap(Map<String, String> oldMap, Map<String, String> newMap) {
        if (oldMap == null || newMap == null) {
            return false;
        }

        if (oldMap.size() != newMap.size()) {
            return false;
        }

        for (String k : oldMap.keySet()) {
            String oldVal = oldMap.get(k);
            String newVal = newMap.get(k);
            if (!Objects.equals(oldVal, newVal)) {
                return false;
            }
        }
        return true;
    }

    public String checkPwd(Request request, String password, String... infos) {
        if (PASSWORD_FLAG.equals(password)) {
            return null;
        }
        int minLength = 10;
        int maxLength = 20;
        if (password.length() < minLength || password.length() > maxLength) {
            return String.format(getAppContext().getAppMetadata().getI18n(request.getI18nLang(), "validator.lengthBetween"), minLength, maxLength);
        }

        if (infos != null && infos.length > 0) {
            if (infos[0] != null) { // for #ITAIT-5014
                if (password.contains(infos[0])) { // 包含身份信息
                    return getAppContext().getAppMetadata().getI18n(request.getI18nLang(), "password.passwordContainsUsername");
                }
            }
        }

        //特殊符号包含下划线
        String PASSWORD_REGEX = "^(?![A-Za-z0-9]+$)(?![a-z0-9_\\W]+$)(?![A-Za-z_\\W]+$)(?![A-Z0-9_\\W]+$)(?![A-Z0-9\\W]+$)[\\w\\W]{10,}$";
        if (!Pattern.compile(PASSWORD_REGEX).matcher(password).matches()) {
            return getAppContext().getAppMetadata().getI18n(request.getI18nLang(), "password.format");
        }

        if (isContinuousChar(password)) { // 连续字符校验
            return getAppContext().getAppMetadata().getI18n(request.getI18nLang(), "password.continuousChars");
        }

        return null;
    }

    /**
     * 是否包含3个及以上相同或字典连续字符
     */
    public static boolean isContinuousChar(String password) {
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

    protected Map<String, String> rectifyParameters(Request request, Map<String, String> newUser, Map<String, String> oldUser) throws Exception {
        String password = newUser.remove(pwdKey);
        newUser.remove(confirmPwdKey);
        boolean passwordChanged = passwordChanged(password);
        if (passwordChanged) {
            String digestAlg = newUser.get("digestAlg");
            String saltLength = newUser.get("saltLength");
            String iterations = newUser.get("iterations");
            MessageDigest messageDigest = MasterApp.getService(CryptoService.class).getMessageDigest();
            newUser.put(pwdKey, messageDigest.digest(password,
                    digestAlg,
                    Integer.parseInt(saltLength),
                    Integer.parseInt(iterations)));
            insertPasswordModifiedTime(newUser);

            String oldPasswords = oldUser.get("oldPasswords");
            String limitRepeats = newUser.get("limitRepeats");
            if (StringUtil.isBlank(limitRepeats)) {
                limitRepeats = oldUser.get("limitRepeats");
            }

            String cutOldPasswords = cutOldPasswords(oldPasswords, limitRepeats, password);
            newUser.put("oldPasswords", cutOldPasswords);
        } else {
            String oldPassword = oldUser.get("password");
            if (StringUtil.notBlank(oldPassword)) {
                newUser.put("password", oldPassword);
            }
        }

        return newUser;
    }

    public static String cutOldPasswords(String oldPasswords, String repeats, String newPwd) {
        if (oldPasswords == null) {
            oldPasswords = "";
        }
        if (StringUtil.notBlank(newPwd)) {
            oldPasswords += (oldPasswords.isEmpty() ? "" : DATA_SEPARATOR) + newPwd;
        }

        if (StringUtil.isBlank(repeats)) {
            repeats = String.valueOf(User.defLimitRepeats);
        }
        int currentLength;
        if (!oldPasswords.contains(DATA_SEPARATOR)) {
            currentLength = 1;
        } else {
            currentLength = oldPasswords.split(DATA_SEPARATOR).length;
        }
        int limitRepeats = Integer.parseInt(repeats);
        int cutCount = currentLength - limitRepeats;
        if (cutCount > 0) {
            for (int i = 0; i < cutCount; i++) {
                int found = oldPasswords.indexOf(DATA_SEPARATOR);
                if (found != -1) {
                    oldPasswords = oldPasswords.substring(found + DATA_SEPARATOR.length());
                }
            }
        }
        return oldPasswords;
    }

    private boolean checkForbidden(Request request, Response response) {
        String id = request.getId();
        if (StringUtil.notBlank(id)) {
            if (Constants.DEFAULT_ADMINISTRATOR.contains(id)) {
                if (Createable.ACTION_NAME_ADD.equals(request.getActionName())
                        || Deletable.ACTION_NAME_DELETE.equals(request.getActionName())) {
                    response.setSuccess(false);
                    response.setMsg(getAppContext().getAppMetadata().getI18n(request.getI18nLang(), "operate.system.users.not"));
                    return false;
                }

                if (Editable.ACTION_NAME_UPDATE.equals(request.getActionName())) {
                    if (!Boolean.parseBoolean(request.getParameter("active"))) {
                        response.setSuccess(false);
                        response.setMsg(getAppContext().getAppMetadata().getI18n(request.getI18nLang(), "System.users.keep.active"));
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @ModelAction(
            name = Deletable.ACTION_NAME_DELETE,
            effectiveWhen = "id!=qingzhou",
            icon = "trash", showToList = true,
            nameI18n = {"删除", "en:Delete"},
            infoI18n = {"删除这个组件，该组件引用的其它组件不会被删除。注：请谨慎操作，删除后不可恢复。",
                    "en:Delete this component, other components referenced by this component will not be deleted. Note: Please operate with caution, it cannot be recovered after deletion."})
    public void delete(Request request, Response response) throws Exception {
        if (!checkForbidden(request, response)) {
            return;
        }

        DataStore dataStore = getDataStore();
        dataStore.deleteDataById(request.getModelName(), request.getId());
    }

    static void insertPasswordModifiedTime(Map<String, String> params) {
        String value = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        params.put("passwordLastModifiedTime", value);
    }

    private boolean passwordChanged(String password) {
        return password != null && !PASSWORD_FLAG.equals(password);
    }
}
