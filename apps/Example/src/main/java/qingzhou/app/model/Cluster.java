package qingzhou.app.model;

import qingzhou.api.*;
import qingzhou.app.AddModelBase;
import qingzhou.app.ExampleMain;

@Model(code = "cluster", icon = "node",
        menu = ExampleMain.MENU_1, order = "4",
        name = {"集群", "en:Cluster"},
        info = {"集群数据管理", "en:Cluster."})
public class Cluster extends AddModelBase {
    public Cluster() {
        super("id");
    }

    @ModelField(required = true,
            search = true,
            id = true,
            color = {"admin:Green"},
            name = {"集群名称", "en:Username"})
    public String id;

    @ModelAction(
            code = "user", icon = "location-arrow",
            list_action = true,
            sub_menu_models = {User.code},
            action_type = ActionType.sub_menu,
            name = {"跳1菜单", "en:User"},
            info = {"打开只有用户菜单的管理页面。", "en:Go to the page."})
    public void user(Request request) {
    }

    @ModelAction(
            code = "org", icon = "location-arrow",
            list_action = true,
            sub_menu_models = {"department", "post"},
            action_type = ActionType.sub_menu,
            name = {"跳2菜单", "en:Org"},
            info = {"打开有部门和岗位菜单的管理页面，默认展开第一个，即部门的模块入口页面。", "en:Go to the page."})
    public void org(Request request) {
    }
}
