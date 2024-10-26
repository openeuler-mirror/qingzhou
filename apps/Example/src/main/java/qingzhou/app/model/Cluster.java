package qingzhou.app.model;

import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.app.AddModelBase;
import qingzhou.app.ExampleMain;

@Model(code = "cluster", icon = "node",
        menu = ExampleMain.MENU_1, order = 1,
        name = {"集群", "en:Cluster"},
        info = {"集群数据管理", "en:Cluster."})
public class Cluster extends AddModelBase {
    @ModelField(
            required = true,
            search = true,
            color = {"admin:Green"},
            name = {"集群名称", "en:Username"})
    public String id;

    @ModelAction(
            code = "user", icon = "location-arrow",
            link_models = {"user"},
            name = {"用户", "en:User"},
            info = {"打开只有用户菜单的管理页面。", "en:Go to the page."})
    public void user(Request request) {
    }

    @ModelAction(
            code = "org", icon = "location-arrow",
            link_models = {"department", "post"},
            name = {"组织", "en:Org"},
            info = {"打开有部门和岗位菜单的管理页面，默认展开第一个，即部门的模块入口页面。", "en:Go to the page."})
    public void org(Request request) {
    }

    @Override
    public String[] listActions() {
        return new String[]{"user", "org"};
    }
}
