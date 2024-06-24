package qingzhou.app.master.system;

import qingzhou.api.AppContext;
import qingzhou.api.DataStore;
import qingzhou.api.FieldType;
import qingzhou.api.Group;
import qingzhou.api.Groups;
import qingzhou.api.Lang;
import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.api.type.Createable;
import qingzhou.api.type.Deletable;
import qingzhou.api.type.Editable;
import qingzhou.api.type.Listable;
import qingzhou.app.master.MasterApp;
import qingzhou.app.master.Password;
import qingzhou.config.Config;
import qingzhou.console.Totp;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.crypto.CryptoServiceFactory;
import qingzhou.engine.util.crypto.MessageDigest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Model(code = "user", icon = "user",
        menu = "System", order = 1,
        name = {"系统用户", "en:System User"},
        info = {"管理登录和操作服务器的用户，用户可登录控制台、REST接口等。", "en:Manages the user who logs in and operates the server. The user can log in to the console, REST interface, etc."})
public class User extends ModelBase implements Createable {
    public static final String pwdKey = "password";
    public static final String confirmPwdKey = "confirmPassword";
    public static final String DATA_SEPARATOR = ",";
    public static final String PASSWORD_FLAG = "***************";

    private static final int defSaltLength = 4;
    private static final int defIterations = 5;

    @Override
    public void start() {
        appContext.addI18n("System.users.keep.active", new String[]{"系统内置用户需要保持启用", "en:System built-in users need to keep active"});
        appContext.addI18n("operate.system.users.not", new String[]{"为安全起见，请勿操作系统内置用户", "en:For security reasons, do not operate the system built-in users"});
    }

    @ModelField(
            group = "basic",
            list = true,
            name = {"用户名", "en:User Name"},
            info = {"用于登录系统的用户名。", "en:The username used to log in to the system."})
    public String id;

    @ModelField(
            group = "basic",
            list = true, required = false,
            name = {"描述", "en:Description"},
            info = {"描述信息。", "en:Description information."})
    public String info = "";

    @ModelField(
            group = "basic", type = FieldType.password, lengthMax = 2048,
            name = {"密码", "en:Password"},
            info = {"密码", "en:Password"})
    public String password;

    @ModelField(
            group = "basic", type = FieldType.password, lengthMax = 2048,
            name = {"确认密码", "en:Confirm Password"},
            info = {"确认登录系统的新密码。", "en:Confirm the new password for logging in to the system."})
    public String confirmPassword;

    @ModelField(
            group = "basic", type = FieldType.select,
            options = {"SHA-256", "SHA-384", "SHA-512"},
            name = {"摘要算法", "en:Digest Algorithm"},
            info = {"进行摘要加密所采用的算法。", "en:The algorithm used for digest encryption."}
    )
    public String digestAlg = "SHA-256";

    @ModelField(
            group = "basic",
            required = false, type = FieldType.number,
            min = 1,
            max = 128,
            name = {"加盐长度", "en:Salt Length"},
            info = {"将自动生成的盐值和字符串一起加密可以提高加密强度。", "en:Encrypting the automatically generated salt value along with the string increases the encryption strength."}
    )
    public Integer saltLength = defSaltLength;

    @ModelField(
            group = "basic",
            required = false, type = FieldType.number,
            min = 1,
            max = 128,
            name = {"迭代次数", "en:Iterations"},
            info = {"连续多次摘要加密可以提高加密强度。", "en:The encryption strength can be improved by multiple digest encryption."}
    )
    public Integer iterations = defIterations;

    @ModelField(
            type = FieldType.bool,
            group = "security",
            required = false,
            name = {"须修改初始密码", "en:Change Initial Password"},
            info = {"安全起见，开启该功能后，初始密码须修改以后才能登录系统。",
                    "en:For security reasons, the initial password must be changed before you can log in to the system once this function is enabled."})
    public Boolean changeInitPwd = true;

    @ModelField(
            group = "security",
            required = false, type = FieldType.bool,
            list = true,
            name = {"双因子认证", "en:Two-factor Authentication"},
            info = {"用户开启双因子认证后，在登录系统时，除验证用户的登录密码之外，还会验证用户的动态密码（由用户终端的双因子认证客户端设备产生），全部验证通过后才允许登录系统。开启双因子认证会自动初始化密钥，该密钥需要在用户的双因子认证客户端上同步绑定（通常是用户通过手机扫描密码修改页面的二维码来进行）。",
                    "en:After a user enables two-factor authentication, when logging into the system, in addition to verifying the user login password, the user dynamic password (generated by the two-factor authentication client device of the user terminal) will also be verified, and the system will be allowed to log in after all verifications are passed. . Enabling two-factor authentication will automatically initialize the key, which needs to be synchronously bound on the user two-factor authentication client (usually the user scans the QR code on the password modification page through the mobile phone)."}
    )
    public Boolean enable2FA = false;

    @ModelField(
            group = "security", type = FieldType.bool,
            required = false, list = true,
            name = {"是否激活", "en:Is Active"},
            info = {"若未激活，则无法登录服务器。", "en:If it is not activated, you cannot log in to the server."})
    public Boolean active = true;

    @ModelField(
            group = "security",
            createable = false, editable = false,
            required = false,
            list = true,
            name = {"密码最后修改时间", "en:Password Last Modified"},
            info = {"最后一次修改密码的日期和时间。", "en:The date the password was last changed."}
    )
    public String passwordLastModified;

    @Override
    public Groups groups() {
        return Groups.of(Group.of("basic", new String[]{"基本属性", "en:Basic"}), Group.of("security", new String[]{"安全", "en:Security"}));
    }

    private String validate(Request request, String fieldName) throws Exception {
        if (pwdKey.equals(fieldName)) {
            String password = request.getParameter(pwdKey);
            if (passwordChanged(password)) {
                String userName = request.getParameter("name");
                String msg = checkPwd(appContext, request.getLang(), password, userName);
                if (msg != null) {
                    return msg;
                }
            }
        }

        if (confirmPwdKey.equals(fieldName)) {
            String password = request.getParameter(pwdKey);
            if (passwordChanged(password)) {
                // 恢复 ITAIT-5005 的修改
                if (!Objects.equals(password, request.getParameter(confirmPwdKey))) {
                    return appContext.getI18n(request.getLang(), "confirmPassword.different");
                }
            }
        }

        return null;
    }

    public static String checkPwd(AppContext appContext, Lang lang, String password, String... infos) {
        if (PASSWORD_FLAG.equals(password)) {
            return null;
        }

        int minLength = 10;
        int maxLength = 20;
        if (password.length() < minLength || password.length() > maxLength) {
            return String.format(appContext.getI18n(lang, "password.lengthBetween"), minLength, maxLength);
        }

        if (infos != null && infos.length > 0) {
            if (infos[0] != null) { // for #ITAIT-5014
                if (password.contains(infos[0])) { // 包含身份信息
                    return appContext.getI18n(lang, "password.passwordContainsUsername");
                }
            }
        }

        //特殊符号包含下划线
        String PASSWORD_REGEX = "^(?![A-Za-z0-9]+$)(?![a-z0-9_\\W]+$)(?![A-Za-z_\\W]+$)(?![A-Z0-9_\\W]+$)(?![A-Z0-9\\W]+$)[\\w\\W]{10,}$";
        if (!Pattern.compile(PASSWORD_REGEX).matcher(password).matches()) {
            return appContext.getI18n(lang, "password.format");
        }

        if (isContinuousChar(password)) { // 连续字符校验
            return appContext.getI18n(lang, "password.continuousChars");
        }

        return null;
    }

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

    @ModelAction(
            name = {"添加", "en:Add"},
            info = {"按配置要求创建一个模块。", "en:Create a module as configured."})
    public void add(Request request, Response response) throws Exception {
        if (!checkForbidden(request, response)) {
            return;
        }

        if (getDataStore().exists(request.getParameter(Listable.FIELD_NAME_ID))) {
            response.setSuccess(false);
            response.setMsg(appContext.getI18n(request.getLang(), "validator.exist"));
            return;
        }

        Map<String, String> newUser = request.getParameters();
        String validate;
        for (String name : newUser.keySet()) {
            validate = validate(request, name);
            if (validate != null) {
                response.setSuccess(false);
                response.setMsg(validate);
                return;
            }
        }

        rectifyParameters(newUser, new HashMap<>());
        getDataStore().addData(newUser.get(Listable.FIELD_NAME_ID), newUser);
    }

    @ModelAction(
            name = {"编辑", "en:Edit"},
            info = {"获得可编辑的数据或界面。", "en:Get editable data or interfaces."})
    public void edit(Request request, Response response) throws Exception {
        show(request, response);
    }

    @ModelAction(
            name = {"查看", "en:Show"},
            info = {"查看该组件的相关信息。", "en:View the information of this model."})
    public void show(Request request, Response response) throws Exception {
        DataStore dataStore = getDataStore();
        Map<String, String> data = dataStore.getDataById(request.getId());
        if (Editable.ACTION_NAME_EDIT.equals(request.getAction())) {
            String[] passwords = Password.splitPwd(data.get("password"));
            String digestAlg = passwords[0];
            int saltLength = Integer.parseInt(passwords[1]);
            int iterations = Integer.parseInt(passwords[2]);
            data.put("digestAlg", digestAlg);
            data.put("saltLength", String.valueOf(saltLength));
            data.put("iterations", String.valueOf(iterations));
            data.put("password", PASSWORD_FLAG);
            data.put("confirmPassword", PASSWORD_FLAG);
        }
        response.addData(data);
    }

    @ModelAction(
            name = {"更新", "en:Update"},
            info = {"更新这个模块的配置信息。", "en:Update the configuration information for this module."})
    public void update(Request request, Response response) throws Exception {
        if (!checkForbidden(request, response)) {
            return;
        }

        DataStore dataStore = getDataStore();
        String userId = request.getId();
        Map<String, String> oldUser = dataStore.getDataById(userId);
        Map<String, String> newUser = request.getParameters();
        String validate;
        for (String name : newUser.keySet()) {
            validate = validate(request, name);
            if (validate != null) {
                response.setSuccess(false);
                response.setMsg(validate);
                return;
            }
        }

        rectifyParameters(newUser, oldUser);
        dataStore.updateDataById(userId, newUser);

        Map<String, String> newPro = dataStore.getDataById(userId);

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

    protected Map<String, String> rectifyParameters(Map<String, String> newUser, Map<String, String> oldUser) {
        String password = newUser.remove(pwdKey);
        newUser.remove(confirmPwdKey);
        boolean passwordChanged = passwordChanged(password);
        if (passwordChanged) {
            String digestAlg = newUser.getOrDefault("digestAlg", "SHA-256");
            String saltLength = newUser.getOrDefault("saltLength", String.valueOf(defSaltLength));
            String iterations = newUser.getOrDefault("iterations", String.valueOf(defIterations));
            MessageDigest messageDigest = CryptoServiceFactory.getInstance().getMessageDigest();
            newUser.put(pwdKey, messageDigest.digest(password,
                    digestAlg,
                    Integer.parseInt(saltLength),
                    Integer.parseInt(iterations)));
            insertPasswordModifiedTime(newUser);

            String historyPasswords = oldUser.get("historyPasswords");
            int limitRepeats = MasterApp.getService(Config.class).getConsole().getSecurity().getPasswordLimitRepeats();
            String cutOldPasswords = cutOldPasswords(historyPasswords, limitRepeats, newUser.get(pwdKey));
            newUser.put("historyPasswords", cutOldPasswords);
        } else {
            String oldPassword = oldUser.get("password");
            if (oldPassword != null) {
                newUser.put("password", oldPassword);
            }
        }

        return newUser;
    }

    public static String cutOldPasswords(String historyPasswords, int limitRepeats, String newPwd) {
        if (historyPasswords == null) {
            historyPasswords = "";
        }
        if (!newPwd.isEmpty()) {
            historyPasswords += (historyPasswords.isEmpty() ? "" : DATA_SEPARATOR) + newPwd;
        }

        int currentLength;
        if (!historyPasswords.contains(DATA_SEPARATOR)) {
            currentLength = 1;
        } else {
            currentLength = historyPasswords.split(DATA_SEPARATOR).length;
        }
        int cutCount = currentLength - limitRepeats;
        if (cutCount > 0) {
            for (int i = 0; i < cutCount; i++) {
                int found = historyPasswords.indexOf(DATA_SEPARATOR);
                if (found != -1) {
                    historyPasswords = historyPasswords.substring(found + DATA_SEPARATOR.length());
                }
            }
        }
        return historyPasswords;
    }

    private boolean checkForbidden(Request request, Response response) {
        String id = request.getId();
        if (id != null) {
            if ("qingzhou".contains(id)) {
                if (Createable.ACTION_NAME_ADD.equals(request.getAction())
                        || Deletable.ACTION_NAME_DELETE.equals(request.getAction())) {
                    response.setSuccess(false);
                    response.setMsg(this.appContext.getI18n(request.getLang(), "operate.system.users.not"));
                    return false;
                }

                if (Editable.ACTION_NAME_UPDATE.equals(request.getAction())) {
                    if (!Boolean.parseBoolean(request.getParameter("active"))) {
                        response.setSuccess(false);
                        response.setMsg(this.appContext.getI18n(request.getLang(), "System.users.keep.active"));
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @ModelAction(
            show = "id!=qingzhou",
            name = {"删除", "en:Delete"},
            info = {"删除这个组件，该组件引用的其它组件不会被删除。注：请谨慎操作，删除后不可恢复。",
                    "en:Delete this component, other components referenced by this component will not be deleted. Note: Please operate with caution, it cannot be recovered after deletion."})
    public void delete(Request request, Response response) throws Exception {
        if (!checkForbidden(request, response)) {
            return;
        }
        String id = request.getId();
        DataStore dataStore = getDataStore();
        dataStore.deleteDataById(id);
    }

    public static void insertPasswordModifiedTime(Map<String, String> params) {
        String value = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        params.put("passwordLastModified", value);
    }

    private boolean passwordChanged(String password) {
        return password != null && !PASSWORD_FLAG.equals(password);
    }

    @Override
    public DataStore getDataStore() {
        return USER_DATA_STORE;
    }

    public static final DataStore USER_DATA_STORE = new UserDataStore();

    private static class UserDataStore implements DataStore {
        @Override
        public List<Map<String, String>> getAllData() throws Exception {
            List<Map<String, String>> users = new ArrayList<>();
            for (qingzhou.config.User user : MasterApp.getService(Config.class).getConsole().getUser()) {
                users.add(Utils.getPropertiesFromObj(user));
            }
            return users;
        }

        @Override
        public void addData(String id, Map<String, String> user) throws Exception {
            qingzhou.config.User u = new qingzhou.config.User();
            Utils.setPropertiesToObj(u, user);
            MasterApp.getService(Config.class).addUser(u);
        }

        @Override
        public void updateDataById(String id, Map<String, String> data) throws Exception {
            Config config = MasterApp.getService(Config.class);
            qingzhou.config.User user = config.getConsole().getUser(id);
            config.deleteUser(id);
            Utils.setPropertiesToObj(user, data);
            config.addUser(user);
        }

        @Override
        public void deleteDataById(String id) throws Exception {
            MasterApp.getService(Config.class).deleteUser(id);
        }
    }

    public static String refresh2FA() {
        return Totp.randomSecureKey();
    }
}
