package qingzhou.app.model;

import qingzhou.api.*;
import qingzhou.api.type.Echo;
import qingzhou.api.type.Option;
import qingzhou.app.MemoryDataStoreModelBase;

import java.util.Map;

@Model(code = Department.code, icon = "sitemap",
        menu = qingzhou.app.ExampleMain.MENU_1, order = "2",
        name = {"部门", "en:Department"},
        info = {"对系统中的部门进行管理，以方便项目登录人员的管理。", "en:Manage departments in the system to facilitate the management of project logged in personnel."})
public class Department extends MemoryDataStoreModelBase implements Echo, Option {
    public static final String code = "department";

    public Department() {
        super("id");
    }

    @ModelField(
            required = true,
            search = true,
            id = true,
            name = {"部门名称", "en:Department Name"},
            info = {"该部门的详细名称。", "en:The name of the department."})
    public String id;

    @ModelField(
            input_type = InputType.radio,
            required = true, dynamic_option = true,
            list = true, search = true, echo_group = {"aa"},
            name = {"上级部门", "en:Superior Department"},
            color = {"a:red", "b:green", "c:blue", "e:yellow"},
            info = {"该部门所属的上级部门。",
                    "en:The superior department to which the department belongs."})
    public String superior = "";

    @ModelField(
            input_type = InputType.checkbox, static_option = true,
            list = true, search = true, echo_group = {"bb"},
            name = {"负责人", "en:Department Manager"},
            color = {"lisa:red", "jack:green", "tom:blue"},
            info = {"该部门的负责人姓名。", "en:Name of the head of the department."})
    public String manager;

    @ModelField(
            input_type = InputType.sortable_checkbox,
            list = true, search = true,
            name = {"排序复选", "en:Department Manager"},
            info = {"排序复选。", "en:Name of the head of the department."})
    public String sortableCheckbox;

    @ModelField(
            input_type = InputType.multiselect,
            list = true, search = true, static_option = true,
            name = {"多选下拉", "en:Department Manager"},
            info = {"多选下拉。", "en:Name of the head of the department."})
    public String multiselect;

    @ModelField(
            input_type = InputType.grouped_multiselect,
            list = true, search = true, static_option = true,
            name = {"分组多选下拉", "en:"})
    public String groupedMultiselect;
    @ModelField(
            list = true, search = true,
            echo_group = {"cc"}, placeholder = "写负责人的电话",
            same_line = true, required = true,
            show_label = false,
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
            input_type = InputType.select,
            update_action = "update", static_option = true,
            same_line = true, required = true, placeholder = "aaaHolder",
            list = true, search = true, show_label = false,
            name = {"邮箱后缀", "en:Email Suffix"},
            info = {"邮箱后缀。", "en:Email Suffix."})
    public String emailSuffix;

    @ModelField(
            same_line = true, show_label = false,
            name = {"备注", "en:remark"},
            info = {"备注。", "en:remark."})
    public String remark;

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
            input_type = InputType.range_datetime,
            list = true, search = true,
            name = {"日期范围", "en:dateRange"}
    )
    public long dateRange;

    @Override
    public void echoData(String echoGroup, Map<String, String> params, DataBuilder dataBuilder) {
        if ("aa".equals(echoGroup)) {
            dataBuilder.addData("manager", "tt", new Item[]{Item.of("tt", new String[]{"天天"}), Item.of("lisa", new String[]{"丽莎"})});
            dataBuilder.addData("sortableCheckbox", "test", new Item[]{Item.of("test", new String[]{"测试"}), Item.of("test2", new String[]{"测试2"})});
            dataBuilder.addData("emailSuffix", "@yahoo.com", new Item[]{Item.of("@yahoo.com", new String[]{"雅虎"}), Item.of("@github.com", new String[]{"哈布"})});
            dataBuilder.addData("multiselect", "multi6", new Item[]{Item.of("multi4", new String[]{"多选4"}), Item.of("multi5", new String[]{"多选5"}), Item.of("multi6", new String[]{"多选6"})});
            dataBuilder.addData("groupedMultiselect", "1_001", new Item[]{Item.of("1", new String[]{"开发分组", "en:Dev"}),
                    Item.of("1_001", new String[]{"开发", "en:Dev"})});
        } else if ("bb".equals(echoGroup)) {
            dataBuilder.addData("superior", "a", new Item[]{Item.of("a", new String[]{"啊"}), Item.of("b", new String[]{"不"})});
            dataBuilder.addData("sortableCheckbox", "test2", new Item[]{Item.of("test", new String[]{"测试"}), Item.of("test2", new String[]{"测试2"})});
            dataBuilder.addData("emailSuffix", "@yahoo.com", new Item[]{Item.of("@yahoo.com", new String[]{"雅虎"}), Item.of("@github.com", new String[]{"哈布"})});
            dataBuilder.addData("groupedMultiselect", "2_002", new Item[]{Item.of("2", new String[]{"测试分组", "en:Test"}),
                    Item.of("2_002", new String[]{"测试", "en:Test"})});
        }
    }

    @Override
    public Item[] optionData(String id, String fieldName) {
        if ("superior".equals(fieldName)) {
            return Item.of(new String[]{"a", "b", "c", "d", "e"});
        } else if ("manager".equals(fieldName)) {
            return new Item[]{Item.of("tt", new String[]{"天天"}), Item.of("lisa", new String[]{"丽莎"}), Item.of("jack", new String[]{"杰克"}), Item.of("tom", new String[]{"汤姆"})};
        } else if ("emailSuffix".equals(fieldName)) {
            return Item.of(new String[]{"@qq.com", "@163.com", "@gmail.com", "@outlook.com", "@yahoo.com", "@github.com"});
        } else if ("multiselect".equals(fieldName)) {
            return new Item[]{Item.of("multi1", new String[]{"多选1"}), Item.of("multi2", new String[]{"多选2"}), Item.of("multi3", new String[]{"多选3"}), Item.of("multi4", new String[]{"多选4"}), Item.of("multi5", new String[]{"多选5"}), Item.of("multi6", new String[]{"多选6"})};
        } else if ("groupedMultiselect".equals(fieldName)) {
            return new Item[]{
                    Item.of("1", new String[]{"开发分组", "en:Dev"}),
                    Item.of("1_001", new String[]{"开发", "en:Dev"}),
                    Item.of("2", new String[]{"测试分组", "en:Test"}),
                    Item.of("2_002", new String[]{"测试", "en:Test"}),
                    Item.of("3", new String[]{"运维分组", "en:ops"}),
                    Item.of("3_003", new String[]{"运维", "en:ops"}),
            };
        }
        return new Item[0];
    }

    @Override
    public Map<String, String> editData(String id) {
        return showData(id);
    }

    @ModelAction(
            code = "menuHealthCheck", icon = "chat-dot",
            name = {"菜单健康检查", "en:Menu Health Check"},
            info = {"为菜单配置健康检查提示信息。", "en:Configure health check prompts for menus."})
    public void menuHealthCheck(Request request) throws Exception {
        request.getResponse().setMsg("Success，这里应该使用国际化输出一些提示信息！");
        request.getResponse().setMsgLevel(MsgLevel.WARN);
    }
}
