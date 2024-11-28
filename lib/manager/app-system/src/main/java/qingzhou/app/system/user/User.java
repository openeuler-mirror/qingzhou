package qingzhou.app.system.user;

import qingzhou.api.*;
import qingzhou.api.type.Delete;
import qingzhou.api.type.General;
import qingzhou.api.type.Option;
import qingzhou.api.type.Validate;
import qingzhou.app.system.Main;
import qingzhou.app.system.ModelUtil;
import qingzhou.core.DeployerConstants;
import qingzhou.core.config.Config;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.MessageDigest;
import qingzhou.engine.util.Utils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

@Model(code = DeployerConstants.MODEL_USER, icon = "user",
        menu = Main.Setting, order = "1",
        name = {"账户", "en:User"},
        info = {"管理登录和操作服务器的账户，账户可登录控制台、REST接口等。", "en:Manages the user who logs in and operates the server. The user can log in to the console, REST interface, etc."})
public class User extends ModelBase implements General, Validate, Option {
    static final String ID_KEY = "name";
    static final String PASSWORD_FLAG = "***************";

    @Override
    public String idField() {
        return ID_KEY;
    }

    @Override
    public boolean contains(String id) {
        String[] ids = allIds(null);
        for (String s : ids) {
            if (s.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private String[] allIds(Map<String, String> query) {
        return Arrays.stream(Main.getService(Config.class).getCore().getConsole().getUser())
                .filter(user -> ModelUtil.query(query, new ModelUtil.Supplier() {
                    @Override
                    public String getModelName() {
                        return DeployerConstants.MODEL_USER;
                    }

                    @Override
                    public Map<String, String> get() {
                        return ModelUtil.getPropertiesFromObj(user);
                    }
                }))
                .map(qingzhou.core.config.User::getName)
                .toArray(String[]::new);
    }

    @Override
    public void start() {
        getAppContext().addI18n("System.users.keep.active", new String[]{"系统内置用户需要保持启用", "en:System built-in users need to keep active"});
        getAppContext().addI18n("confirmPassword.different", new String[]{"账户密码与确认密码不一致", "en:The account password is inconsistent with the confirmation password"});
        getAppContext().addI18n("password.format", new String[]{"密码须包含大小写字母、数字、特殊符号，长度至少 10 位。", "en:Password must contain uppercase and lowercase letters, numbers, special symbols, and must be at least 10 characters long"});
        getAppContext().addI18n("password.passwordContainsUsername", new String[]{"密码不能包含用户名", "en:A weak password, the password cannot contain the username"});
        getAppContext().addI18n("password.continuousChars", new String[]{"密码不能包含三个或三个以上相同或连续的字符", "en:A weak password, the password cannot contain three or more same or consecutive characters"});
    }

    @ModelField(
            required = true,
            search = true,
            name = {"账户名称", "en:User Name"},
            info = {"用于登录系统的用户名。", "en:The username used to log in to the system."})
    public String name;

    @ModelField(
            input_type = InputType.password,
            required = true, show = false,
            min_length = 10, max_length = 20,
            name = {"账户密码", "en:Password"},
            info = {"用于登录系统的账户密码。", "en:The account password used to log in to the system."})
    public String password;

    @ModelField(
            input_type = InputType.password,
            required = true, show = false,
            min_length = 10, max_length = 20,
            name = {"确认密码", "en:Confirm Password"},
            info = {"确认登录系统的新密码。", "en:Confirm the new password for logging in to the system."})
    public String confirmPassword;

    @ModelField(
            show = false,
            input_type = InputType.select,
            name = {"摘要算法", "en:Digest Algorithm"},
            info = {"进行摘要加密所采用的算法。", "en:The algorithm used for digest encryption."}
    )
    public String digestAlg = "SHA-256";

    @ModelField(
            show = false,
            input_type = InputType.number,
            min = 1,
            max = 128,
            name = {"加盐长度", "en:Salt Length"},
            info = {"将自动生成的盐值和字符串一起加密可以提高加密强度。", "en:Encrypting the automatically generated salt value along with the string increases the encryption strength."}
    )
    public Integer saltLength = 4;

    @ModelField(
            show = false,
            input_type = InputType.number,
            min = 1,
            max = 128,
            name = {"迭代次数", "en:Iterations"},
            info = {"连续多次摘要加密可以提高加密强度。", "en:The encryption strength can be improved by multiple digest encryption."}
    )
    public Integer iterations = 5;

    @ModelField(
            input_type = InputType.bool,
            name = {"下次登录须改密码", "en:Change Initial Password"},
            info = {"标记该用户下次登录系统后，须首先修改其登录密码，否则不能进行其它操作。",
                    "en:After marking the user to log in to the system next time, he or she must first change his login password, otherwise no other operations can be performed."})
    public Boolean changePwd = true;

    @ModelField(
            create = false, edit = false,
            list = true, search = true,
            name = {"密码最后修改时间", "en:Password Last Modified"},
            info = {"最后一次修改密码的日期和时间。", "en:The date the password was last changed."}
    )
    public String passwordLastModified;

    @ModelField(
            input_type = InputType.bool,
            list = true, search = true,
            color = {"true:Green", "false:Gray"},
            name = {"启用", "en:Active"},
            info = {"若未启用，则无法登录服务器。", "en:If it is not activated, you cannot log in to the server."})
    public Boolean active = true;

    @ModelField(
            input_type = InputType.select,
            required = true, search = true,
            list = true,
            ref_model = Role.class,
            update_action = "update",
            name = {"角色", "en:Role"},
            info = {"为用户分配角色。", "en:Assign roles to users."})
    public String role;

    @ModelField(
            list = true, search = true,
            link_action = "show",
            name = {"描述", "en:Description"},
            info = {"此账户的说明信息。", "en:Description of this account."})
    public String info;

    @Override
    public Map<String, String> showData(String id) {
        Map<String, String> data = showDataForUserInternal(id);
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
        MessageDigest messageDigest = getAppContext().getService(CryptoService.class).getMessageDigest();
        data.put("password", messageDigest.digest(data.get("password"),
                digestAlg,
                Integer.parseInt(saltLength),
                Integer.parseInt(iterations)));

        // 添加密码更新时间戳
        insertPasswordModifiedTime(data);

        qingzhou.core.config.User u = new qingzhou.core.config.User();
        ModelUtil.setPropertiesToObj(u, data);
        Main.getService(Config.class).addUser(u);
    }

    @Override
    public void updateData(Map<String, String> data) throws Exception {
        // 去除不需要持久化的参数
        data.remove("confirmPassword");

        // 对新密码进行加密
        String password = data.remove("password");
        if (passwordChanged(password)) {
            Map<String, String> originUser = showDataForUserInternal(data.get(ID_KEY));

            String[] splitOriginPwd = splitPwd(originUser.get("password"));
            String digestAlg = data.getOrDefault("digestAlg", splitOriginPwd[0]);
            String saltLength = data.getOrDefault("saltLength", splitOriginPwd[1]);
            String iterations = data.getOrDefault("iterations", splitOriginPwd[2]);

            MessageDigest messageDigest = getAppContext().getService(CryptoService.class).getMessageDigest();
            data.put("password", messageDigest.digest(password,
                    digestAlg,
                    Integer.parseInt(saltLength),
                    Integer.parseInt(iterations)));

            insertPasswordModifiedTime(data);

            String historyPasswords = originUser.get("historyPasswords");
            int limitRepeats = Main.getService(Config.class).getCore().getConsole().getSecurity().getPasswordLimitRepeats();
            String cutOldPasswords = cutOldPasswords(historyPasswords, limitRepeats, data.get("password"));
            data.put("historyPasswords", cutOldPasswords);
        }

        // 持久化
        updateDataForUser(data);
    }

    @Override
    public void deleteData(String id) throws Exception {
        String[] batchId = getAppContext().getCurrentRequest().getBatchId();
        if (batchId != null && batchId.length > 0) {
            Main.getService(Config.class).deleteUser(batchId);
        } else {
            Main.getService(Config.class).deleteUser(id);
        }
    }

    @Override
    public List<String[]> listData(int pageNum, int pageSize, String[] showFields, Map<String, String> query) throws IOException {
        return ModelUtil.listData(allIds(query), this::showData, pageNum, pageSize, showFields);
    }

    @ModelAction(
            code = Delete.ACTION_DELETE, icon = "trash",
            show = "name!=qingzhou",
            batch_action = true,
            list_action = true, order = "9", action_type = ActionType.action_list, distribute = true,
            name = {"删除", "en:Delete"},
            info = {"删除本条数据，注：请谨慎操作，删除后不可恢复。",
                    "en:Delete this data, note: Please operate with caution, it cannot be restored after deletion."})
    public void delete(Request request) throws Exception {
        getAppContext().invokeSuperAction(request);
    }

    private boolean passwordChanged(String password) {
        return password != null && !password.equals(PASSWORD_FLAG);
    }

    static Map<String, String> showDataForUserInternal(String userId) {
        for (qingzhou.core.config.User user : Main.getService(Config.class).getCore().getConsole().getUser()) {
            if (user.getName().equals(userId)) {
                Map<String, String> data = ModelUtil.getPropertiesFromObj(user);
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
        String id = data.get(ID_KEY);
        qingzhou.core.config.User user = config.getCore().getConsole().getUser(id);
        config.deleteUser(id);
        if (PASSWORD_FLAG.equals(data.get("password"))) {
            data.remove("password");
        }
        ModelUtil.setPropertiesToObj(user, data);
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

    @Override
    public Map<String, String> validate(Request request) {
        Map<String, String> errors = new HashMap<>();
        String password = request.getParameter("password");

        boolean isAddOrUpdate = Boolean.parseBoolean(request.getParameter(Validate.IS_ADD_OR_UPDATE_NON_MODEL_PARAMETER));
        if (isAddOrUpdate) {
            String msg = checkPwd(password, request.getUser());
            if (msg != null) {
                errors.put("password", getAppContext().getI18n(msg));
            }
        } else {
            String userId = request.getId();
            if (DeployerConstants.DEFAULT_USER_QINGZHOU.equals(userId)) {
                String active = request.getParameter("active");
                if (active != null && !Boolean.parseBoolean(active)) {
                    errors.put("active", getAppContext().getI18n("System.users.keep.active"));
                }
            }

            if (passwordChanged(password)) {
                String msg = checkPwd(password, userId);
                if (msg != null) {
                    errors.put("password", getAppContext().getI18n(msg));
                }
            }
        }

        if (!Objects.equals(password, request.getParameter("confirmPassword"))) {
            String error = getAppContext().getI18n("confirmPassword.different");
            errors.put("password", error);
            errors.put("confirmPassword", error);
        }

        return errors;
    }

    @Override
    public String[] staticOptionFields() {
        return new String[]{"digestAlg"};
    }

    @Override
    public String[] dynamicOptionFields() {
        return null;
    }

    @Override
    public Item[] optionData(String fieldName) {
        if ("digestAlg".equals(fieldName)) {
            return Item.of(new String[]{"SHA-256", "SHA-384", "SHA-512"});
        }
        return null;
    }

    @Override
    public boolean showOrderNumber() {
        return false;
    }
}
