package qingzhou.app.oauth2;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import qingzhou.api.InputType;
import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.action.Add;
import qingzhou.api.action.Delete;
import qingzhou.api.action.Page;
import qingzhou.api.action.Show;
import qingzhou.api.action.Update;

@Model(code = "user", order = 2,
        name = {"资源拥有者", "en:User"},
        info = {"可登录授权页的账号", "en:Accounts that can sign in on the consent page"},
        icon = "User",
        menu = "access")
public class UserModel extends ModelBase implements Page, Show, Add, Update, Delete {

    @ModelField(id = true,
            name = {"编号", "en:ID"},
            info = {"用户记录编号", "en:User record id"},
            list = true,
            readonly = true)
    public String id;

    @ModelField(
            name = {"用户名", "en:User Name"},
            info = {"授权页登录用户名", "en:Login name on consent page"},
            list = true,
            search = true,
            required = true)
    public String userName;

    @ModelField(input_type = InputType.password,
            name = {"密码", "en:Password"},
            info = {"授权页登录密码", "en:Login password on consent page"},
            required = true)
    public String password;

    @ModelField(
            name = {"昵称", "en:Nickname"},
            info = {"用户昵称", "en:User nickname"},
            list = true)
    public String nickname;

    @ModelField(
            name = {"角色", "en:Role"},
            info = {"userinfo 返回的角色", "en:Role returned by userinfo"},
            list = true)
    public String roleName;

    private Store store;

    @Override
    public void start() {
        try {
            store = Store.get(getAppContext());
        } catch (Throwable e) {
            getAppContext().getService(qingzhou.logger.Logger.class)
                    .error("failed to init oauth2 store", e);
        }
    }

    @Override
    public List<String[]> page(int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception {
        return store.page(Store.USER_TABLE, "userName", pageNum, pageSize, ClientModel.keyword(query, "userName"), listFields);
    }

    @Override
    public int totalSize(Map<String, String> query) {
        try {
            return store.count(Store.USER_TABLE, "userName", ClientModel.keyword(query, "userName"));
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean contains(String id) {
        return true;
    }

    @Override
    public Map<String, String> show(String id) throws Exception {
        Map<String, String> user = store.show(Store.USER_TABLE, id);
        if (user == null) return null;

        // 列名在库中折叠为大写、Store 读取时转小写，这里需还原为 @ModelField 的 code
        Map<String, String> result = new HashMap<>();
        result.put("id", user.get("id"));
        result.put("userName", user.get("username"));
        result.put("nickname", user.get("nickname"));
        result.put("roleName", user.get("rolename"));
        return result; // 不含 password：编辑时不回传明文口令，留空即保留原值
    }

    @Override
    public void add(Map<String, String> data) throws Exception {
        store.insert(Store.USER_TABLE, data);
    }

    @Override
    public void update(String id, Map<String, String> data) throws Exception {
        if (Security.isEmpty(data.get("password"))) {
            data.remove("password"); // 未修改密码时保留原值
        }
        store.update(Store.USER_TABLE, id, data);
    }

    @Override
    public void delete(String id) throws Exception {
        store.delete(Store.USER_TABLE, id);
    }
}
