package qingzhou.app.model;

import qingzhou.api.*;
import qingzhou.api.type.Option;
import qingzhou.app.AddModelBase;
import qingzhou.app.ExampleMain;

import java.util.HashMap;
import java.util.Map;

@Model(code = "cluster", icon = "node",
        menu = ExampleMain.MENU_1, order = 1,
        name = {"集群", "en:Cluster"},
        info = {"集群数据管理", "en:Cluster."})
public class Cluster extends AddModelBase implements Option {
    @ModelField(
            required = true,
            search = true,
            color = {"admin:Green"},
            name = {"集群名称", "en:Username"})
    public String id;

    @ModelField(
            input_type = InputType.select,
            search = true,
            field_type = FieldType.OTHER,
            name = {"时间", "en:Time"})
    public String time;

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


    @Override
    public Map<String, String> defaultSearch() {
        return new HashMap<String, String>() {{
            put("time", "30");
        }};
    }

    @Override
    public String[] staticOptionFields() {
        return new String[]{"time"};
    }

    @Override
    public String[] dynamicOptionFields() {
        return new String[0];
    }

    @Override
    public Item[] optionData(String fieldName) {
        if ("time".equals(fieldName)) {
            return new Item[]{
                    Item.of("10", new String[]{"最近10分钟", "en:10min"}),
                    Item.of("30", new String[]{"最近半小时", "en:30min"}),
                    Item.of("60", new String[]{"最近1小时", "en:1h"}),
            };
        }
        return null;
    }
}
