package qingzhou.app.system.user;

import qingzhou.api.*;
import qingzhou.api.type.Delete;
import qingzhou.api.type.Echo;
import qingzhou.api.type.General;
import qingzhou.app.system.Main;
import qingzhou.app.system.ModelUtil;
import qingzhou.app.system.business.App;
import qingzhou.core.DeployerConstants;
import qingzhou.core.config.Config;
import qingzhou.core.deployer.Deployer;
import qingzhou.core.registry.AppInfo;
import qingzhou.core.registry.ModelActionInfo;
import qingzhou.core.registry.ModelInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Model(code = Role.MODEL_NAME, icon = "check-sign",
        menu = Main.Setting, order = "2",
        name = {"角色", "en:Role"},
        info = {"定义应用的操作权限。", "en:Define the operation permissions of the app."})
public class Role extends ModelBase implements General, Echo {
    public static final String MODEL_NAME = "role";
    static final String ID_KEY = "name";

    @ModelField(
            required = true,
            search = true,
            name = {"名称", "en:Name"},
            info = {"该角色的唯一标识。", "en:Unique identifier of the role."})
    public String name;

    @ModelField(
            input_type = InputType.select,
            required = true, search = true,
            ref_model = App.class,
            echo_group = "uri",
            name = {"应用", "en:App"},
            info = {"指定该角色的权限作用到的应用。", "en:Specify the apps to which the role permissions apply."})
    public String app;

    @ModelField(input_type = InputType.multiselect,
            name = {"权限", "en:Permissions"},
            info = {"角色的权限表示具有该角色的用户可以访问的资源（URI）集合。", "en:The permissions of a role represent a collection of resources (URI) that users with that role can access."})
    public String uris;

    @ModelField(input_type = InputType.bool,
            list = true, search = true,
            color = {"true:Green", "false:Gray"},
            name = {"是否激活", "en:Is Active"},
            info = {"是否激活该角色，激活后，拥有该角色的用户将可以访问该角色对应的资源，否则用户将无法访问对应的资源。",
                    "en:Whether to activate the role, after activation, users with this role will be able to access the resources corresponding to the role, otherwise the users will not be able to access the corresponding resources."})
    public Boolean active = true;

    @ModelField(
            search = true, list = true,
            name = {"描述", "en:Description"},
            info = {"该角色的描述说明信息。", "en:Description of the role."})
    public String info = "";

    @ModelAction(
            code = Delete.ACTION_DELETE, icon = "trash",
            batch_action = true,
            list_action = true, order = "9", action_type = ActionType.action_list, distribute = true,
            name = {"删除", "en:Delete"},
            info = {"删除本条数据，注：请谨慎操作，删除后不可恢复。",
                    "en:Delete this data, note: Please operate with caution, it cannot be restored after deletion."})
    public void delete(Request request) throws Exception {
        getAppContext().invokeSuperAction(request);
    }

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
        qingzhou.core.config.Role[] roles = Main.getService(Config.class).getCore().getConsole().getRole();
        if (roles == null) return new String[0];

        return Arrays.stream(roles)
                .filter(role -> ModelUtil.query(query, new ModelUtil.Supplier() {
                    @Override
                    public String getModelName() {
                        return "role";
                    }

                    @Override
                    public Map<String, String> get() {
                        return ModelUtil.getPropertiesFromObj(role);
                    }
                }))
                .map(qingzhou.core.config.Role::getName)
                .toArray(String[]::new);
    }

    @Override
    public Map<String, String> showData(String id) {
        for (qingzhou.core.config.Role role : Main.getService(Config.class).getCore().getConsole().getRole()) {
            if (role.getName().equals(id)) {
                return ModelUtil.getPropertiesFromObj(role);
            }
        }
        return null;
    }

    @Override
    public void addData(Map<String, String> data) throws Exception {
        qingzhou.core.config.Role role = new qingzhou.core.config.Role();
        ModelUtil.setPropertiesToObj(role, data);
        Main.getService(Config.class).addRole(role);
    }

    @Override
    public void updateData(Map<String, String> data) throws Exception {
        Config config = Main.getService(Config.class);
        String id = data.get(ID_KEY);
        qingzhou.core.config.Role role = config.getCore().getConsole().getRole(id);
        config.deleteRole(id);
        ModelUtil.setPropertiesToObj(role, data);
        config.addRole(role);
    }

    @Override
    public void deleteData(String id) throws Exception {
        String[] batchId = getAppContext().getCurrentRequest().getBatchId();
        if (batchId != null && batchId.length > 0) {
            for (String bId : batchId) {
                Main.getService(Config.class).deleteRole(bId);
            }
        } else {
            Main.getService(Config.class).deleteRole(id);
        }
    }

    @Override
    public List<String[]> listData(int pageNum, int pageSize, String[] showFields, Map<String, String> query) throws IOException {
        return ModelUtil.listData(allIds(query), this::showData, pageNum, pageSize, showFields);
    }

    @Override
    public void echoData(String echoGroup, Map<String, String> params, DataBuilder dataBuilder) {
        String appName = params.get("app");
        Deployer deployer = Main.getService(Deployer.class);
        AppInfo appInfo = deployer.getAppInfo(appName);
        if (appInfo == null) return;

        List<Item> itemList = new ArrayList<>();
        for (ModelInfo model : appInfo.getModelInfos()) {
            for (ModelActionInfo action : model.getModelActionInfos()) {
                Item item = Item.of(model.getCode() + DeployerConstants.MULTISELECT_GROUP_SEPARATOR + action.getCode(), action.getName());
                itemList.add(item);
            }
        }
        dataBuilder.addData("uris", "", itemList.toArray(new Item[0]));
    }
}
