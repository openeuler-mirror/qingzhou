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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Model(code = DeployerConstants.MODEL_ROLE, icon = "check-sign",
        menu = Main.Setting, order = "2",
        name = {"角色", "en:Role"},
        info = {"管理操作应用的系统管理员角色。", "en:The system administrator role of managing operation applications."})
public class Role extends ModelBase implements General, Echo {
    static final String ID_KEY = "name";

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
        return Arrays.stream(Main.getService(Config.class).getCore().getConsole().getRole())
                .filter(role -> ModelUtil.query(query, new ModelUtil.Supplier() {
                    @Override
                    public String getModelName() {
                        return DeployerConstants.MODEL_ROLE;
                    }

                    @Override
                    public Map<String, String> get() {
                        return ModelUtil.getPropertiesFromObj(role);
                    }
                }))
                .map(qingzhou.core.config.Role::getName)
                .toArray(String[]::new);
    }


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
            update_action = "update",
            echo_group = "application",
            name = {"应用", "en:App"},
            info = {"角色所属应用，角色配置在该应用中才生效。", "en:The role configuration takes effect only on the application to which the role belongs."})
    public String app;

    @ModelField(
            search = true, list = true,
            name = {"描述", "en:Description"},
            info = {"该角色的描述说明信息。", "en:Description of the role."})
    public String info = "";

    @ModelField(input_type = InputType.bool,
            list = true, search = true,
            color = {"true:Green", "false:Gray"},
            name = {"是否激活", "en:Is Active"},
            info = {"是否激活该角色，激活后，拥有该角色的用户将可以访问该角色对应的资源，否则用户将无法访问对应的资源。",
                    "en:Whether to activate the role, after activation, users with this role will be able to access the resources corresponding to the role, otherwise the users will not be able to access the corresponding resources."})
    public Boolean active = true;

    @ModelField(input_type = InputType.multiselect,
            name = {"权限", "en:Permissions"},
            info = {"角色的权限表示具有该角色的用户可以访问的资源（URI）集合。", "en:The permissions of a role represent a collection of resources (URI) that users with that role can access."})
    public String uris;

    @Override
    public Map<String, String> showData(String id) {
        return showDataForRoleInternal(id);
    }

    @Override
    public void addData(Map<String, String> data) throws Exception {
        qingzhou.core.config.Role role = new qingzhou.core.config.Role();
        ModelUtil.setPropertiesToObj(role, data);
        Main.getService(Config.class).addRole(role);
    }

    @Override
    public void updateData(Map<String, String> data) throws Exception {
        // 持久化
        updateDataForRole(data);
    }

    @Override
    public void deleteData(String id) throws Exception {
        String[] batchId = getAppContext().getCurrentRequest().getBatchId();
        if (batchId != null && batchId.length > 0) {
            Main.getService(Config.class).deleteRole(batchId);
        } else {
            Main.getService(Config.class).deleteRole(id);
        }
    }

    @Override
    public List<String[]> listData(int pageNum, int pageSize, String[] showFields, Map<String, String> query) throws IOException {
        return ModelUtil.listData(allIds(query), this::showData, pageNum, pageSize, showFields);
    }

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


    static Map<String, String> showDataForRoleInternal(String roleId) {
        for (qingzhou.core.config.Role role : Main.getService(Config.class).getCore().getConsole().getRole()) {
            if (role.getName().equals(roleId)) {
                return ModelUtil.getPropertiesFromObj(role);
            }
        }
        return null;
    }

    static void updateDataForRole(Map<String, String> data) throws Exception {
        Config config = Main.getService(Config.class);
        String id = data.get(ID_KEY);
        qingzhou.core.config.Role role = config.getCore().getConsole().getRole(id);
        config.deleteRole(id);
        ModelUtil.setPropertiesToObj(role, data);
        config.addRole(role);
    }

    @Override
    public void echoData(String echoGroup, Map<String, String> params, DataBuilder dataBuilder) throws Exception {
        if ("application".equals(echoGroup)) {
            String appName = params.get("app");
            Deployer deployer = Main.getService(Deployer.class);
            AppInfo appInfo = deployer.getAppInfo(appName);
            //todo 权限列表数据回显
            for (ModelInfo modelInfo : appInfo.getModelInfos()) {
                ModelActionInfo[] modelActionInfos = modelInfo.getModelActionInfos();
            }

            dataBuilder.addData("uris", "", new Item[]{Item.of("multi4", new String[]{"多选4"}), Item.of("multi5", new String[]{"多选5"}), Item.of("multi6", new String[]{"多选6"})});
        }
    }

    @Override
    public boolean showOrderNumber() {
        return false;
    }
}
