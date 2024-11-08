package qingzhou.app.model;

import qingzhou.api.InputType;
import qingzhou.api.Item;
import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.type.Echo;
import qingzhou.api.type.Option;
import qingzhou.app.AddModelBase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Model(code = "department", icon = "sitemap",
        menu = qingzhou.app.ExampleMain.MENU_1, order = 2,
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
            input_type = InputType.radio,
            required = true,
            list = true, search = true, echo_group = {"aa"},
            name = {"上级部门", "en:Superior Department"},
            color = {"a:red","b:green","c:blue","e:yellow"},
            info = {"该部门所属的上级部门。",
                    "en:The superior department to which the department belongs."})
    public String superior = "";

    @ModelField(
            input_type = InputType.checkbox,
            list = true, search = true, echo_group = {"aa", "bb"}, same_lines = {"phone", "buildDate"},
            name = {"负责人", "en:Department Manager"},
            color = {"lisa:red", "jack:green", "tom:blue"},
            info = {"该部门的负责人姓名。", "en:Name of the head of the department."})
    public String manager;

    @ModelField(
            list = true, search = true,
            echo_group = {"cc"},
            color = {"AAAAA:Green",
                    "BBBBB:Gray"},
            name = {"联系电话", "en:Department Phone"},
            info = {"该部门的联系电话。", "en:The department contact number."})
    public String phone;

    @ModelField(
            list = true, search = true,
            email = true,
            same_lines = {"emailSuffix"},
            name = {"电子邮箱", "en:Department Email"},
            info = {"可以与该部门取得联系的电子邮箱。", "en:An E-mail address where the department can be contacted."})
    public String email;

    @ModelField(
            input_type = InputType.select,
            update_action = "update",
            list = true, search = true, showLabel = false,
            name = {"邮箱后缀", "en:Email Suffix"},
            info = {"邮箱后缀。", "en:Email Suffix."})
    public String emailSuffix;

    @ModelField(
            input_type = InputType.bool,
            list = true, search = true,
            color = {"true:Green", "false:Gray"},
            name = {"启用", "en:Active"},
            info = {"。", "en:."})
    public Boolean active = true;

    @ModelField(
            input_type = InputType.datetime,
            list = true, search = true,
            name = {"建立日期", "en:Date"}
    )
    public long buildDate;

    @ModelField(
            input_type = InputType.range_datetime, showLabel = false,
            list = true, search = true,
            name = {"日期范围", "en:dateRange"}
    )
    public long dateRange;

    @Override
    public Map<String, String> echoData(String echoGroup, Map<String, String> params) {
        Map<String, String> map = new HashMap<>();
        if ("aa".equals(echoGroup)) {
            String superior = params.get("superior");
            String manager = params.get("manager");
            map.put("email", superior + "===" + manager);
            map.put("phone", superior + "&&&" + manager);
            if ("a".equals(superior) || "c".equals(superior)) {
                map.put("active", "false");
                map.put("emailSuffix", "@qq.com");
            } else {
                map.put("active", "true");
                map.put("emailSuffix", "@163.com");
            }
            map.put("buildDate", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        } else if ("bb".equals(echoGroup)) {
        } else if ("cc".equals(echoGroup)) {
            map.put("buildDate", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        }
        return map;
    }


    @Override
    public String[] staticOptionFields() {
        return new String[]{"manager", "emailSuffix"};
    }

    @Override
    public String[] dynamicOptionFields() {
        return new String[]{"superior"};
    }

    @Override
    public Item[] optionData(String fieldName) {
        if ("superior".equals(fieldName)) {
            return Item.of(new String[]{"a", "b", "c", "d", "e"});
        } else if ("manager".equals(fieldName)) {
            return Item.of(new String[]{"jack", "lisa", "tom"});
        } else if ("emailSuffix".equals(fieldName)) {
            return Item.of(new String[]{"@qq.com", "@163.com", "@gmail.com", "@outlook.com", "@yahoo.com", "@tongtech.com", "@github.com"});
        }
        return new Item[0];
    }

    @Override
    public Map<String, String> editData(String id) throws Exception {
        Map<String, String> map = showData(id);
        map.put("emailSuffix", null);
        return map;
    }
}
