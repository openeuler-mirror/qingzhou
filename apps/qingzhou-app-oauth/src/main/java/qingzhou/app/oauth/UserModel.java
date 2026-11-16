package qingzhou.app.oauth;

import java.sql.SQLException;
import java.util.*;

import qingzhou.api.InputType;
import qingzhou.api.ModelBase;
import qingzhou.api.action.*;
import qingzhou.api.annotations.*;
import qingzhou.logger.Logger;

@Model(code = "user", order = 2,
        name = {"资源拥有者", "en:User"},
        info = {"授权页和密码模式使用的资源拥有者账号，与接入客户端相互独立；也是 OIDC 用户信息来源",
                "en:Resource owner accounts for consent-page/password login, independent of OAuth clients"},
        icon = "User",
        menu = "access")
@OverrideDefaultActions({@ModelAction(code = "add", icon = "Check", order = 1, add = true,
        name = {"保存", "en:Save"},
        info = {"保存新建用户；校验失败时直接返回可读错误", "en:Create the user; validation errors are returned to the form"}),
        @ModelAction(code = "update", icon = "Check", order = 1, update = true,
                name = {"更新", "en:Update"},
                info = {"更新用户；校验失败时直接返回可读错误", "en:Update the user; validation errors are returned to the form"})})
public class UserModel extends ModelBase implements Page, Show, Delete, Add, Update {
    @ModelField(id = true, list = true, readonly = true,
            name = {"编号", "en:ID"},
            info = {"用户记录编号", "en:User record id"})
    public String id;

    @ModelField(list = true, search = true, required = true,
            name = {"用户名", "en:User Name"},
            info = {"授权页/密码模式登录用户名；不是接入客户端的 client_id",
                    "en:Login name for consent/password flow, not an OAuth client_id"})
    public String userName;

    @ModelField(input_type = InputType.password,
            name = {"密码", "en:Password"},
            info = {"新增时必填；编辑时留空保留原值。不是接入客户端的 client_secret",
                    "en:Required when creating; blank on edit keeps the old value. Not an OAuth client_secret"})
    public String password;

    @ModelField(list = true,
            name = {"昵称", "en:Nickname"},
            info = {"用户昵称", "en:User nickname"})
    public String nickname;

    @ModelField(list = true, email = true,
            name = {"邮箱", "en:Email"},
            info = {"OIDC email scope 返回的邮箱地址", "en:Email returned by the OIDC email scope"})
    public String email;

    @ModelField(list = true,
            name = {"角色", "en:Role"},
            info = {"userinfo 返回的 role/roles 字段值，可按业务自定义，如 system/auditor",
                    "en:Role returned by userinfo, e.g. system or auditor"})
    public String roleName;

    @ModelField(list = true, input_type = InputType.bool,
            name = {"启用", "en:Enabled"},
            info = {"关闭后该用户不能登录授权页，也不能通过密码模式换取令牌",
                    "en:Disabled users cannot sign in or use the password grant"})
    public String enabled = "true";

    private Store store;

    @Override
    public void start() {
        try {
            store = Store.get(getAppContext());
        } catch (Throwable e) {
            Logger logger = getAppContext().getService(Logger.class);
            if (logger != null) logger.error("failed to init oauth store", e);
        }
    }

    @Override
    public List<String[]> page(int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception {
        return store.page(Store.USER_TABLE, "username", pageNum, pageSize,
                ClientModel.keyword(query, "userName"), listFields);
    }

    @Override
    public int total(Map<String, String> query) {
        try {
            return store.count(Store.USER_TABLE, "username", ClientModel.keyword(query, "userName"));
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean contains(String id) {
        try {
            return store.exists(Store.USER_TABLE, id);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Map<String, String> show(String id) throws Exception {
        Map<String, String> user = store.show(Store.USER_TABLE, id);
        if (user == null) return null;

        // 数据库/内存统一使用小写列名，返回时还原为 @ModelField 的 code
        Map<String, String> result = new HashMap<>();
        result.put("id", user.get("id"));
        result.put("userName", user.get("username"));
        result.put("nickname", user.get("nickname"));
        result.put("email", user.get("email"));
        result.put("roleName", user.get("rolename"));
        result.put("enabled", user.getOrDefault("enabled", "true"));
        return result; // 不含 password：编辑时不回传摘要，留空即保留原值
    }

    @Override
    public void add(Map<String, String> data) throws Exception {
        FormSupport.save(getCurrentRequest(), (FormSupport.Operation) () -> {
            String userName = data.get("userName");
            if (Security.isEmpty(userName)) throw new IllegalArgumentException("userName 不能为空");
            if (store.findUser(userName) != null) throw new IllegalArgumentException("userName 已存在: " + userName);
            if (Security.isEmpty(data.get("password"))) throw new IllegalArgumentException("password 不能为空");
            if (Security.isEmpty(data.get("enabled"))) data.put("enabled", "true");
            store.insert(Store.USER_TABLE, data);
        });
    }

    @Override
    public void update(String id, Map<String, String> data) throws Exception {
        FormSupport.save(getCurrentRequest(), (FormSupport.Operation) () -> {
            Map<String, String> existing = store.show(Store.USER_TABLE, id);
            if (existing == null) throw new IllegalArgumentException("用户不存在: " + id);

            String userName = data.get("userName");
            if (!Security.isEmpty(userName) && !userName.equals(existing.get("username"))
                    && store.findUser(userName) != null) {
                throw new IllegalArgumentException("userName 已存在: " + userName);
            }
            if (Security.isEmpty(data.get("password"))) data.remove("password");
            store.update(Store.USER_TABLE, id, data);
        });
    }

    @Override
    public void delete(String id) throws Exception {
        store.delete(Store.USER_TABLE, id);
    }
}
