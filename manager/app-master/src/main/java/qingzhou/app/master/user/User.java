package qingzhou.app.master.user;

import qingzhou.api.*;
import qingzhou.api.type.Addable;
import qingzhou.app.master.Main;
import qingzhou.config.Config;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.MessageDigest;
import qingzhou.deployer.DeployerConstants;
import qingzhou.engine.util.Utils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

@Model(code = "user", icon = "user",
        menu = "System", order = 1,
        name = {"账户", "en:User"},
        info = {"管理登录和操作服务器的账户，账户可登录控制台、REST接口等。", "en:Manages the user who logs in and operates the server. The user can log in to the console, REST interface, etc."})
public class User extends ModelBase implements Addable {
    static final String idKey = "id";
    static final String PASSWORD_FLAG = "***************";

    @Override
    public String idFieldName() {
        return idKey;
    }

    @Override
    public void start() {
        appContext.addI18n("System.users.keep.active", new String[]{"系统内置用户需要保持启用", "en:System built-in users need to keep active"});
        appContext.addI18n("confirmPassword.different", new String[]{"输入的确认密码与密码不一致", "en:Confirm that the password does not match the new password"});
        appContext.addI18n("password.format", new String[]{"密码须包含大小写字母、数字、特殊符号，长度至少 10 位。", "en:Password must contain uppercase and lowercase letters, numbers, special symbols, and must be at least 10 characters long"});
        appContext.addI18n("password.passwordContainsUsername", new String[]{"密码不能包含用户名", "en:A weak password, the password cannot contain the username"});
        appContext.addI18n("password.continuousChars", new String[]{"密码不能包含三个或三个以上相同或连续的字符", "en:A weak password, the password cannot contain three or more same or consecutive characters"});
    }

    @ModelField(
            required = true,
            list = true,
            name = {"账户名称", "en:User Name"},
            info = {"用于登录系统的用户名。", "en:The username used to log in to the system."})
    public String id;

    @ModelField(
            type = FieldType.password,
            required = true,
            lengthMin = 10, lengthMax = 20,
            name = {"账户密码", "en:Password"},
            info = {"用于登录系统的账户密码。", "en:The account password used to log in to the system."})
    public String password;

    @ModelField(
            type = FieldType.password,
            required = true,
            lengthMin = 10, lengthMax = 20,
            name = {"确认密码", "en:Confirm Password"},
            info = {"确认登录系统的新密码。", "en:Confirm the new password for logging in to the system."})
    public String confirmPassword;

    @ModelField(
            type = FieldType.select,
            options = {"SHA-256", "SHA-384", "SHA-512"},
            name = {"摘要算法", "en:Digest Algorithm"},
            info = {"进行摘要加密所采用的算法。", "en:The algorithm used for digest encryption."}
    )
    public String digestAlg = "SHA-256";

    @ModelField(
            type = FieldType.number,
            min = 1,
            max = 128,
            name = {"加盐长度", "en:Salt Length"},
            info = {"将自动生成的盐值和字符串一起加密可以提高加密强度。", "en:Encrypting the automatically generated salt value along with the string increases the encryption strength."}
    )
    public Integer saltLength = 4;

    @ModelField(
            type = FieldType.number,
            min = 1,
            max = 128,
            name = {"迭代次数", "en:Iterations"},
            info = {"连续多次摘要加密可以提高加密强度。", "en:The encryption strength can be improved by multiple digest encryption."}
    )
    public Integer iterations = 5;

    @ModelField(
            type = FieldType.bool,
            name = {"下次登录须改密码", "en:Change Initial Password"},
            info = {"标记该用户下次登录系统后，须首先修改其登录密码，否则不能进行其它操作。",
                    "en:After marking the user to log in to the system next time, he or she must first change his login password, otherwise no other operations can be performed."})
    public Boolean changePwd = true;

    @ModelField(
            createable = false, editable = false,
            list = true,
            name = {"密码最后修改时间", "en:Password Last Modified"},
            info = {"最后一次修改密码的日期和时间。", "en:The date the password was last changed."}
    )
    public String passwordLastModified;

    @ModelField(
            type = FieldType.bool,
            list = true,
            name = {"启用", "en:Active"},
            info = {"若未启用，则无法登录服务器。", "en:If it is not activated, you cannot log in to the server."})
    public Boolean active = true;

    @ModelField(
            list = true,
            name = {"描述", "en:Description"},
            info = {"此账户的说明信息。", "en:Description of this account."})
    public String info;

    @Override
    public Map<String, String> showData(String id) throws Exception {
        Map<String, String> data = Objects.requireNonNull(showDataForUser(id));
        data.put("password", PASSWORD_FLAG);
        data.put("confirmPassword", PASSWORD_FLAG);
        return data;
    }

    @Override
    public void addData(Map<String, String> data) throws Exception {
        // 去除不需要持久化的参数
        data.remove("confirmPassword");

        // 对新密码进行加密
        User defaultValue = new User();
        String digestAlg = data.getOrDefault("digestAlg", defaultValue.digestAlg);
        String saltLength = data.getOrDefault("saltLength", String.valueOf(defaultValue.saltLength));
        String iterations = data.getOrDefault("iterations", String.valueOf(defaultValue.iterations));
        MessageDigest messageDigest = appContext.getService(CryptoService.class).getMessageDigest();
        data.put("password", messageDigest.digest(data.get("password"),
                digestAlg,
                Integer.parseInt(saltLength),
                Integer.parseInt(iterations)));

        // 添加密码更新时间戳
        insertPasswordModifiedTime(data);

        qingzhou.config.User u = new qingzhou.config.User();
        Utils.setPropertiesToObj(u, data);
        Main.getService(Config.class).addUser(u);
    }

    @Override
    public void updateData(Map<String, String> data) throws Exception {
        // 去除不需要持久化的参数
        data.remove("confirmPassword");

        // 对新密码进行加密
        String password = data.remove("password");
        if (passwordChanged(password)) {
            Map<String, String> originUser = showData(data.get(idKey));

            String[] splitOriginPwd = splitPwd(originUser.get("password"));
            String digestAlg = data.getOrDefault("digestAlg", splitOriginPwd[0]);
            String saltLength = data.getOrDefault("saltLength", splitOriginPwd[1]);
            String iterations = data.getOrDefault("iterations", splitOriginPwd[2]);

            MessageDigest messageDigest = appContext.getService(CryptoService.class).getMessageDigest();
            data.put("password", messageDigest.digest(password,
                    digestAlg,
                    Integer.parseInt(saltLength),
                    Integer.parseInt(iterations)));

            insertPasswordModifiedTime(data);

            String historyPasswords = originUser.get("historyPasswords");
            int limitRepeats = Main.getService(Config.class).getConsole().getSecurity().getPasswordLimitRepeats();
            String cutOldPasswords = cutOldPasswords(historyPasswords, limitRepeats, data.get("password"));
            data.put("historyPasswords", cutOldPasswords);
        }

        // 持久化
        updateDataForUser(data);
    }

    @Override
    public void deleteData(String id) throws Exception {
        Main.getService(Config.class).deleteUser(id);
    }

    @Override
    public List<Map<String, String>> listData(int pageNum, int pageSize, String[] fieldNames) throws Exception {
        List<Map<String, String>> users = new ArrayList<>();
        for (qingzhou.config.User user : Main.getService(Config.class).getConsole().getUser()) {
            users.add(Utils.getPropertiesFromObj(user));
        }
        return users;
    }

    @ModelAction(
            code = DeployerConstants.ACTION_ADD,
            ajax = true,
            name = {"添加", "en:Add"},
            info = {"按配置要求创建一个模块。", "en:Create a module as configured."})
    public void add(Request request) throws Exception {
        String msg = checkPwd(request.getParameter("password"), request.getUser());
        if (msg != null) {
            request.getResponse().setSuccess(false);
            request.getResponse().setMsg(this.appContext.getI18n(request.getLang(), msg));
            return;
        }

        appContext.callDefaultAction(request);
    }

    @ModelAction(
            code = DeployerConstants.ACTION_DELETE,
            show = "id!=qingzhou",
            name = {"删除", "en:Delete"},
            info = {"删除本条数据，注：请谨慎操作，删除后不可恢复。",
                    "en:Delete this data, note: Please operate with caution, it cannot be restored after deletion."})
    public void delete(Request request) throws Exception {
        appContext.callDefaultAction(request);
    }

    @ModelAction(
            code = DeployerConstants.ACTION_UPDATE,
            name = {"更新", "en:Update"},
            info = {"更新账户信息。",
                    "en:Update your account information."})
    public void update(Request request) throws Exception {
        String userId = request.getId();
        if ("qingzhou".equals(userId)) {
            if (!Boolean.parseBoolean(request.getParameter("active"))) {
                request.getResponse().setSuccess(false);
                request.getResponse().setMsg(this.appContext.getI18n(request.getLang(), "System.users.keep.active"));
                return;
            }
        }

        String password = request.getParameter("password");
        if (passwordChanged(password)) {
            String msg = checkPwd(password, userId);
            if (msg != null) {
                request.getResponse().setSuccess(false);
                request.getResponse().setMsg(this.appContext.getI18n(request.getLang(), msg));
                return;
            }
            if (!Objects.equals(password, request.getParameter("confirmPassword"))) {
                request.getResponse().setSuccess(false);
                request.getResponse().setMsg(this.appContext.getI18n(request.getLang(), "confirmPassword.different"));
            }
        }

        appContext.callDefaultAction(request);
    }

    private boolean passwordChanged(String password) {
        return password != null && !password.equals(PASSWORD_FLAG);
    }

    static Map<String, String> showDataForUser(String userId) throws Exception {
        for (qingzhou.config.User user : Main.getService(Config.class).getConsole().getUser()) {
            if (user.getId().equals(userId)) {
                Map<String, String> data = Utils.getPropertiesFromObj(user);
                String[] passwords = splitPwd(data.get("password"));
                String digestAlg = passwords[0];
                int saltLength = Integer.parseInt(passwords[1]);
                int iterations = Integer.parseInt(passwords[2]);
                data.put("digestAlg", digestAlg);
                data.put("saltLength", String.valueOf(saltLength));
                data.put("iterations", String.valueOf(iterations));
                return data;
            }
        }
        return null;
    }

    static void updateDataForUser(Map<String, String> data) throws Exception {
        Config config = Main.getService(Config.class);
        String id = data.get(idKey);
        qingzhou.config.User user = config.getConsole().getUser(id);
        config.deleteUser(id);
        if (PASSWORD_FLAG.equals(data.get("password"))) {
            data.remove("password");
        }
        Utils.setPropertiesToObj(user, data);
        config.addUser(user);
    }

    static String checkPwd(String password, String userId) {
        if (Utils.isBlank(password) || PASSWORD_FLAG.equals(password)) return null;

        if (userId != null) {
            if (password.contains(userId)) { // 包含身份信息
                return "password.passwordContainsUsername";
            }
        }

        //特殊符号包含下划线
        String PASSWORD_REGEX = "^(?![A-Za-z0-9]+$)(?![a-z0-9_\\W]+$)(?![A-Za-z_\\W]+$)(?![A-Z0-9_\\W]+$)(?![A-Z0-9\\W]+$)[\\w\\W]{10,}$";
        if (!Pattern.compile(PASSWORD_REGEX).matcher(password).matches()) return "password.format";

        // 连续字符校验
        if (isContinuousChar(password)) return "password.continuousChars";

        return null;
    }

    static String[] splitPwd(String storedCredentials) {
        String SP = "$";
        String[] pwdArray = new String[4];
        int lastIndexOf = storedCredentials.lastIndexOf(SP);

        String digestAlg = storedCredentials.substring(lastIndexOf + 1);
        pwdArray[pwdArray.length - 1] = digestAlg;

        storedCredentials = storedCredentials.substring(0, lastIndexOf);
        String[] oldPwdDigestStyle = storedCredentials.split("\\" + SP);

        oldPwdDigestStyle[1] = String.valueOf(oldPwdDigestStyle[1].length() / 2);
        System.arraycopy(oldPwdDigestStyle, 0, pwdArray, 0, pwdArray.length - 1);

        return pwdArray;
    }

    static String cutOldPasswords(String historyPasswords, int limitRepeats, String newPwd) {
        String DATA_SEPARATOR = DeployerConstants.DEFAULT_DATA_SEPARATOR;

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

    static void insertPasswordModifiedTime(Map<String, String> params) {
        String value = new SimpleDateFormat(DeployerConstants.PASSWORD_LAST_MODIFIED_DATE_FORMAT).format(new Date());
        params.put("passwordLastModified", value);
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
}
