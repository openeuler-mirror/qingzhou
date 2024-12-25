package qingzhou.app.system.system.user;

import java.io.IOException;
import java.util.List;
import java.util.*;

import qingzhou.api.*;
import qingzhou.api.type.*;
import qingzhou.app.system.Main;
import qingzhou.app.system.ModelUtil;
import qingzhou.core.DeployerConstants;
import qingzhou.core.deployer.Deployer;
import qingzhou.core.registry.AppInfo;
import qingzhou.core.registry.ModelActionInfo;
import qingzhou.core.registry.ModelInfo;
import qingzhou.engine.util.Utils;

@Model(code = Role.MODEL_NAME, icon = "check-sign",
        menu = Main.Setting, order = "2",
        name = {"角色", "en:Role"},
        info = {"定义应用的操作权限。", "en:Define the operation permissions of the app."})
public class Role extends ModelBase implements General, Echo, Option {
    public static final String MODEL_NAME = "role";
    static final String ID_KEY = "name";

    @ModelField(
            required = true, search = true,
            id = true,
            name = {"名称", "en:Name"},
            info = {"该角色的唯一标识。", "en:Unique identifier of the role."})
    public String name;

    @ModelField(
            input_type = InputType.select,
            required = true, search = true,
            echo_group = "uri", dynamic_option = true,
            name = {"应用", "en:App"},
            info = {"指定该角色的权限作用到的应用。", "en:Specify the apps to which the role permissions apply."})
    public String app;

    @ModelField(input_type = InputType.grouped_multiselect,
            required = true, search = true,
            dynamic_option = true,
            separator = DeployerConstants.ROLE_URI_SP,
            name = {"权限", "en:Permissions"},
            info = {"角色的权限表示具有该角色的用户可以访问的资源（URI）集合。", "en:The permissions of a role represent a collection of resources (URI) that users with that role can access."})
    public String uris;

    @ModelField(input_type = InputType.bool,
            list = true, search = true,
            update_action = Update.ACTION_UPDATE,
            color = {"true:Green", "false:Gray"},
            name = {"激活", "en:Active"},
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
            list_action = true, order = "9", action_type = ActionType.action_list,
            name = {"删除", "en:Delete"},
            info = {"删除本条数据，注：请谨慎操作，删除后不可恢复。",
                    "en:Delete this data, note: Please operate with caution, it cannot be restored after deletion."})
    public void delete(Request request) throws Exception {
        getAppContext().invokeSuperAction(request);
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
        qingzhou.config.console.Role[] roles = Main.getConsole().getRole();
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
                .map(qingzhou.config.console.Role::getName)
                .toArray(String[]::new);
    }

    @Override
    public Map<String, String> showData(String id) {
        qingzhou.config.console.Role[] roles = Main.getConsole().getRole();
        if (roles != null) {
            for (qingzhou.config.console.Role role : roles) {
                if (role.getName().equals(id)) {
                    return ModelUtil.getPropertiesFromObj(role);
                }
            }
        }
        return null;
    }

    @Override
    public void addData(Map<String, String> data) throws Exception {
        qingzhou.config.console.Role role = new qingzhou.config.console.Role();
        ModelUtil.setPropertiesToObj(role, data);
        Main.getConfig().addRole(role);
    }

    @Override
    public void updateData(Map<String, String> data) throws Exception {
        String id = data.get(ID_KEY);
        qingzhou.config.console.Role role = Main.getConsole().getRole(id);
        Main.getConfig().deleteRole(id);
        ModelUtil.setPropertiesToObj(role, data);
        Main.getConfig().addRole(role);
    }

    @Override
    public void deleteData(String id) throws Exception {
        Main.getConfig().deleteRole(id);
    }

    @Override
    public List<String[]> listData(int pageNum, int pageSize, String[] showFields, Map<String, String> query) throws IOException {
        return ModelUtil.listData(allIds(query), this::showData, pageNum, pageSize, showFields);
    }

    @Override
    public void echoData(String echoGroup, Map<String, String> params, DataBuilder dataBuilder) {
        String appName = params.get("app");
        if (Utils.isBlank(appName)) {
            return;
        }
        List<Item> items = getItems(appName);
        String value = "";
        qingzhou.config.console.Role[] roles = Main.getConsole().getRole();
        if (roles != null) {
            for (qingzhou.config.console.Role role : roles) {
                if (role.getApp().equals(appName)) {
                    value = role.getUris();
                }
            }
        }
        dataBuilder.addData("uris", value, items.toArray(new Item[0]));
    }

    @Override
    public Item[] optionData(String id, String fieldName) {
        if ("app".equals(fieldName)) {
            Deployer deployer = Main.getService(Deployer.class);
            List<Item> appItems = new ArrayList<>();
            for (String app : deployer.getAllApp()) {
                if (app.equals(DeployerConstants.APP_SYSTEM)) {
                    appItems.add(Item.of(app, new String[]{"集中管理", "en:Centralized Management"}));
                } else {
                    appItems.add(Item.of(app));
                }
            }
            return appItems.toArray(new Item[0]);
        }

        if ("uris".equals(fieldName)) {
            Map<String, String> map = showData(id);
            if (map == null) {
                return new Item[0];
            }
            String app = map.get("app");
            List<Item> list = getItems(app);
            return list.toArray(new Item[0]);
        }
        return null;
    }

    private List<Item> getItems(String app) {
        List<Item> list = new ArrayList<>();
        Deployer deployer = Main.getService(Deployer.class);
        AppInfo appInfo = deployer.getAppInfo(app);
        if (appInfo == null) return list;

        for (ModelInfo modelInfo : appInfo.getModelInfos()) {
            if (modelInfo.isHidden()) continue;

            String modelName = modelInfo.getCode();
            if (DeployerConstants.APP_SYSTEM.equals(appInfo.getName())) {
                if (modelName.equals(DeployerConstants.MODEL_AGENT)) continue;
                if (DeployerConstants.NONE_ROLE_SYSTEM_MODELS.contains(modelName)) continue;
            } else {
                if (DeployerConstants.NONE_ROLE_NONE_SYSTEM_MODELS.contains(modelName)) continue;
            }

            list.add(Item.of(modelName, modelInfo.getName()));

            ModelActionInfo[] modelActionInfos = modelInfo.getModelActionInfos();
            Arrays.stream(modelActionInfos).forEach(modelActionInfo -> {
                String action = modelActionInfo.getCode();

                Set<String> set = appInfo.getLoginFreeModelActions().get(modelName);
                if (set != null && set.contains(action)) return;

                if (DeployerConstants.APP_SYSTEM.equals(appInfo.getName())) {
                    Set<String> actions = DeployerConstants.NONE_ROLE_SYSTEM_MODEL_ACTIONS.get(modelName);
                    if (actions != null && actions.contains(action)) {
                        return;
                    }
                }

                String[] nameI18n = modelActionInfo.getName();
                if (nameI18n != null && nameI18n.length > 0) {
                    list.add(Item.of(modelName
                            + DeployerConstants.MULTISELECT_GROUP_SEPARATOR
                            + action, nameI18n));
                }
            });
        }
        return list;
    }
}
