package qingzhou.app.master.system;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import qingzhou.api.DataStore;
import qingzhou.api.FieldType;
import qingzhou.api.Group;
import qingzhou.api.Groups;
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
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.crypto.CryptoServiceFactory;
import qingzhou.engine.util.crypto.MessageDigest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Model(code = "user", icon = "user",
        menu = "System", order = 1,
        name = {"系统用户", "en:System User"},
        info = {"管理登录和操作服务器的用户，用户可登录控制台、REST接口等。", "en:Manages the user who logs in and operates the server. The user can log in to the console, REST interface, etc."})
public class User extends ModelBase implements Createable {
    public static final String pwdKey = "password";
    public static final String confirmPwdKey = "confirmPassword";
    public static final int defSaltLength = 4;
    public static final int defIterations = 5;
    public static final int defLimitRepeats = 5;
    public static final String DATA_SEPARATOR = ",";
    public static final String PASSWORD_FLAG = "***************";

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
            group = "security",
            required = false,
            name = {"须修改初始密码", "en:Change Initial Password"},
            info = {"安全起见，开启该功能后，初始密码须修改以后才能登录系统。",
                    "en:For security reasons, the initial password must be changed before you can log in to the system once this function is enabled."})
    public Boolean changeInitPwd = true;

    @ModelField(
            group = "security", type = FieldType.bool,
            required = false,
            name = {"密码期限", "en:Password Age"},
            info = {"开启该功能，可限制密码的使用期限。",// 内部：0 表示可以永久不更新。
                    "en:Enable this feature to limit the expiration date of the password."}
    )
    public Boolean enablePasswordAge = true;

    @ModelField(
            group = "security", show = "enablePasswordAge=true",
            required = false, type = FieldType.number, min = 1, max = 90,
            name = {"密码最长使用期限", "en:Maximum Password Age"},
            info = {"用户登录系统的密码距离上次修改超过该期限（单位为天）后，需首先更新密码才能继续登录系统。",// 内部：0 表示可以永久不更新。
                    "en:After the password of the user logging in to the system has been last modified beyond this period (in days), the user must first update the password before continuing to log in to the system."}
    )
    public Integer passwordMaxAge = 90;

    @ModelField(
            group = "security",
            required = false, type = FieldType.number,
            min = 0,
            name = {"密码最短使用期限", "en:Minimum Password Age"},
            info = {"用户登录系统的密码距离上次修改未达到该期限（单位为天），则不能进行更新。0 表示可以随时更新。",
                    "en:If the user password for logging in to the system has not reached this period (in days) since the last modification, it cannot be updated. 0 means that it can be updated at any time."}
    )
    public Integer passwordMinAge = 0;

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
    public String passwordLastModifiedTime;

    @ModelField(
            group = "security",
            required = false, type = FieldType.number, min = 1, max = 10,
            name = {"不与最近密码重复", "en:Recent Password Restrictions"},
            info = {"限制本次更新的密码不能和最近几次使用过的密码重复。注：设置为 “1” 表示只要不与当前密码重复即可。",
                    "en:Restrict this update password to not be duplicated by the last few times you have used. Note: A setting of 1 means as long as it does not duplicate the current password."})
    public Integer limitRepeats = defLimitRepeats;

    @ModelField(
            group = "security",
            createable = false, editable = false,
            required = false,
            name = {"历史密码", "en:Historical Passwords"},
            info = {"记录最近几次使用过的密码。", "en:Keep a record of the last few passwords you have used."})
    public String oldPasswords;

    @Override
    public Groups groups() {
        return Groups.of(Group.of("basic", new String[]{"基本属性", "en:Basic"}), Group.of("security", new String[]{"安全", "en:Security"}));
    }

    @ModelAction(
            name = {"添加", "en:Add"},
            info = {"按配置要求创建一个模块。", "en:Create a module as configured."})
    public void add(Request request, Response response) throws Exception {
        if (!checkForbidden(request, response)) {
            return;
        }
        Map<String, String> newUser = request.getParameters();
        rectifyParameters(newUser, new HashMap<>());
        getDataStore().addData(newUser.get(Listable.FIELD_NAME_ID), newUser);
    }


    @ModelAction(
            name = {"查看", "en:Show"},
            info = {"查看该组件的相关信息。", "en:View the information of this model."})
    public void show(Request request, Response response) throws Exception {
        DataStore dataStore = getDataStore();
        Map<String, String> data = dataStore.getDataById(request.getId());
        if (Editable.ACTION_NAME_EDIT.equals(request.getAction())) {
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
        String modelName = request.getModel();
        String userId = request.getId();
        Map<String, String> oldUser = dataStore.getDataById(userId);
        Map<String, String> newUser = request.getParameters();
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
            String digestAlg = newUser.get("digestAlg");
            String saltLength = newUser.get("saltLength");
            String iterations = newUser.get("iterations");
            MessageDigest messageDigest = CryptoServiceFactory.getInstance().getMessageDigest();
            newUser.put(pwdKey, messageDigest.digest(password,
                    digestAlg,
                    Integer.parseInt(saltLength),
                    Integer.parseInt(iterations)));
            insertPasswordModifiedTime(newUser);

            String oldPasswords = oldUser.get("oldPasswords");
            String limitRepeats = newUser.get("limitRepeats");
            if (limitRepeats == null) {
                limitRepeats = oldUser.get("limitRepeats");
            }

            String cutOldPasswords = cutOldPasswords(oldPasswords, limitRepeats, password);
            newUser.put("oldPasswords", cutOldPasswords);
        } else {
            String oldPassword = oldUser.get("password");
            if (oldPassword != null) {
                newUser.put("password", oldPassword);
            }
        }

        return newUser;
    }

    public static String cutOldPasswords(String oldPasswords, String repeats, String newPwd) {
        if (oldPasswords == null) {
            oldPasswords = "";
        }
        if (!newPwd.isEmpty()) {
            oldPasswords += (oldPasswords.isEmpty() ? "" : DATA_SEPARATOR) + newPwd;
        }

        if (repeats.isEmpty()) {
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

        DataStore dataStore = getDataStore();
        dataStore.deleteDataById(request.getId());
    }

    public static void insertPasswordModifiedTime(Map<String, String> params) {
        String value = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        params.put("passwordLastModifiedTime", value);
    }

    private boolean passwordChanged(String password) {
        return password != null && !PASSWORD_FLAG.equals(password);
    }

    @Override
    public DataStore getDataStore() {
        return userDataStore;
    }

    private final UserDataStore userDataStore = new UserDataStore();

    private static class UserDataStore implements DataStore {
        public UserDataStore() {
        }

        private String configFile;

        private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

        @Override
        public List<Map<String, String>> getAllData() throws Exception {
            JsonObject jsonObject = readJsonFile();
            if (jsonObject != null) {
                JsonArray userArray = getUserJsonArray(jsonObject);
                List<Map<String, String>> userList = getUserList(userArray);

                return userList;
            }

            return null;
        }

        @Override
        public void addData(String id, Map<String, String> user) throws Exception {
            JsonObject jsonObject = readJsonFile();

            if (jsonObject != null) {
                JsonArray userArray = getUserJsonArray(jsonObject);

                JsonObject newUserObject = new JsonObject();
                user.forEach(newUserObject::addProperty);
                userArray.add(newUserObject);

                writeJsonFile(jsonObject);
            }
        }

        @Override
        public void updateDataById(String id, Map<String, String> data) throws Exception {
            JsonObject jsonObject = readJsonFile();

            if (jsonObject != null) {
                JsonArray userArray = getUserJsonArray(jsonObject);

                for (JsonElement userElement : userArray) {
                    JsonObject userObject = userElement.getAsJsonObject();
                    if (id.equals(userObject.get("id").getAsString())) {
                        for (String key : data.keySet()) {
                            userObject.addProperty(key, data.get(key));
                        }
                        break;
                    }
                }

                writeJsonFile(jsonObject);
            }
        }

        @Override
        public void deleteDataById(String id) throws Exception {
            if ("qingzhou".equals(id)) {
                return;
            }

            JsonObject jsonObject = readJsonFile();
            if (jsonObject != null) {
                JsonArray userArray = getUserJsonArray(jsonObject);

                JsonArray newUserArray = new JsonArray();
                for (JsonElement userElement : userArray) {
                    JsonObject userObject = userElement.getAsJsonObject();
                    if (!id.equals(userObject.get("id").getAsString())) {
                        newUserArray.add(userObject);
                    }
                }

                jsonObject.getAsJsonObject("module").remove("user");
                jsonObject.getAsJsonObject("module").add("user", newUserArray);

                writeJsonFile(jsonObject);
            }
        }

        private JsonArray getUserJsonArray(JsonObject jsonObject) {
            return jsonObject.getAsJsonObject("module").getAsJsonObject("console").getAsJsonArray("user");
        }

        private static List<Map<String, String>> getUserList(JsonArray userArray) {
            List<Map<String, String>> userList = new ArrayList<>();
            for (JsonElement userElement : userArray) {
                JsonObject userObject = userElement.getAsJsonObject();
                Map<String, String> userMap = new HashMap<>();
                for (Map.Entry<String, JsonElement> entry : userObject.entrySet()) {
                    userMap.put(entry.getKey(), entry.getValue().getAsString());
                }
                userList.add(userMap);
            }
            return userList;
        }

        private JsonObject readJsonFile() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(getConfigFile())), StandardCharsets.UTF_8))) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        private void writeJsonFile(JsonObject jsonObject) {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(Paths.get(getConfigFile())), StandardCharsets.UTF_8))) {
                gson.toJson(jsonObject, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private String getConfigFile() throws IOException {
            if (configFile == null || configFile.isEmpty()) {
                configFile = Utils.newFile(MasterApp.getInstanceDir(), "qingzhou.json").getCanonicalPath();
            }

            return configFile;
        }
    }
}
