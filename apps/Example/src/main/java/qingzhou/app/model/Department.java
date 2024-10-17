package qingzhou.app.model;

import qingzhou.api.FieldType;
import qingzhou.api.Item;
import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.type.Echo;
import qingzhou.api.type.Option;
import qingzhou.app.AddModelBase;

import java.util.HashMap;
import java.util.Map;

@Model(code = "department", icon = "sitemap",
        menu = qingzhou.app.ExampleMain.MENU_11, order = 1,
        name = {"部门", "en:Department"},
        info = {"对系统中的部门进行管理，以方便项目登录人员的管理。", "en:Manage departments in the system to facilitate the management of project logged in personnel."})
public class Department extends AddModelBase implements Echo, Option {
    @ModelField(
            required = true,
            search = true,
            name = {"部门名称", "en:Department Name"},
            info = {"该部门的详细名称。", "en:The name of the department."})
    public String id;

    @ModelField(
            type = FieldType.radio,
            list = true, search = true, echoGroup = {"aa"},
            name = {"上级部门", "en:Superior Department"},
            info = {"该部门所属的上级部门。",
                    "en:The superior department to which the department belongs."})
    public String superior = "";

    @ModelField(
            type = FieldType.checkbox,
            list = true, search = true, echoGroup = {"aa", "bb"},
            name = {"负责人", "en:Department Manager"},
            info = {"该部门的负责人姓名。", "en:Name of the head of the department."})
    public String manager;

    @ModelField(
            list = true, search = true,
            color = {"AAAAA:Green",
                    "BBBBB:Gray"},
            name = {"联系电话", "en:Department Phone"},
            info = {"该部门的联系电话。", "en:The department contact number."})
    public String phone;

    @ModelField(
            list = true, search = true,
            email = true,
            name = {"电子邮箱", "en:Department Email"},
            info = {"可以与该部门取得联系的电子邮箱。", "en:An E-mail address where the department can be contacted."})
    public String email;

    @ModelField(
            type = FieldType.datetime,
            list = true, search = true,
            name = {"建立日期", "en:Date"}
    )
    public long buildDate;

    @Override
    public Map<String, String> echoData(String echoGroup, Map<String, String> params) {
        Map<String, String> map = new HashMap<>();
        if (echoGroup.equals("aa")) {
            String superior = params.get("superior");
            String manager = params.get("manager");
            map.put("email", superior + "===" + manager);
            map.put("phone", superior + "&&&" + manager);
        } else if (echoGroup.equals("bb")) {
            String manager = params.get("manager");
            map.put("phone", manager + "---");
        }
        return map;
    }


    @Override
    public String[] staticOptionFields() {
        return new String[]{"manager"};
    }

    @Override
    public String[] dynamicOptionFields() {
        return new String[]{"superior"};
    }

    @Override
    public Item[] optionData(String fieldName) {
        if (fieldName.equals("superior")) {
            return Item.of(new String[]{"a", "b", "c", "d", "e"});
        } else if (fieldName.equals("manager")) {
            return Item.of(new String[]{"jack", "lisa", "tom"});
        }
        return new Item[0];
    }
}
