package qingzhou.app.oauth2;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.action.*;

@Model(code = "client", order = 1,
        name = {"接入客户端", "en:Client"},
        info = {"OAuth 2.0 接入方凭据与回调地址管理", "en:OAuth 2.0 client credentials and redirect uri"},
        icon = "Grid",
        menu = "access")
public class ClientModel extends ModelBase implements Page, Show, Add, Update, Delete {

    @ModelField(id = true,
            name = {"编号", "en:ID"},
            info = {"客户端记录编号", "en:Client record id"},
            list = true,
            readonly = true)
    public String id;

    @ModelField(
            name = {"客户端 ID", "en:Client ID"},
            info = {"授权请求中的 client_id", "en:client_id of authorization request"},
            list = true,
            search = true,
            required = true)
    public String client_id;

    @ModelField(
            name = {"客户端密钥", "en:Client Secret"},
            info = {"授权服务器的 client_secret", "en:client_secret of authorization server"},
            required = true)
    public String client_secret;

    @ModelField(
            name = {"客户端名称", "en:Client Name"},
            info = {"授权页展示的名称", "en:Name shown on consent page"},
            list = true,
            required = true)
    public String client_name;

    @ModelField(
            name = {"回调地址", "en:Redirect URI"},
            info = {"授权完成后回跳的地址", "en:Redirect uri after authorization"},
            list = true)
    public String redirect_uri;

    @ModelField(
            name = {"授予类型", "en:Grant Types"},
            info = {"该客户端允许的 grant_type，逗号分隔", "en:Allowed grant types, comma separated"})
    public String grant_types;

    @ModelField(
            name = {"授权范围", "en:Scope"},
            info = {"该客户端可申请的 scope", "en:Scopes the client may request"})
    public String scope;

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
        return store.page(Store.CLIENT_TABLE, "client_id", pageNum, pageSize, keyword(query, "client_id"), listFields);
    }

    @Override
    public int totalSize(Map<String, String> query) {
        try {
            return store.count(Store.CLIENT_TABLE, "client_id", keyword(query, "client_id"));
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
        Map<String, String> client = store.show(Store.CLIENT_TABLE, id);
        if (client != null) client.remove("client_secret"); // 不回传密钥（摘要），留空即保留原值
        return client;
    }

    @Override
    public void add(Map<String, String> data) throws Exception {
        store.insert(Store.CLIENT_TABLE, data);
    }

    @Override
    public void update(String id, Map<String, String> data) throws Exception {
        if (Security.isEmpty(data.get("client_secret"))) {
            data.remove("client_secret"); // 未修改密钥时保留原值
        }
        store.update(Store.CLIENT_TABLE, id, data);
    }

    @Override
    public void delete(String id) throws Exception {
        store.delete(Store.CLIENT_TABLE, id);
    }

    static String keyword(Map<String, String> query, String column) {
        if (query == null) return "";
        String keyword = query.get(column);
        return keyword == null ? "" : keyword;
    }
}
