package qingzhou.app.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import qingzhou.api.ActionType;
import qingzhou.api.InputType;
import qingzhou.api.Item;
import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.type.Echo;
import qingzhou.api.type.Group;
import qingzhou.api.type.Option;
import qingzhou.app.AddModelBase;
import qingzhou.app.ExampleMain;


@Model(code = "user", icon = "user",
        menu = ExampleMain.MENU_1, order = 1,
        name = {"用户", "en:User management"},
        info = {"用户管理", "en:User management."})
public class User extends AddModelBase implements Group, Option, Echo {
    @ModelField(
            group = "base",
            required = true,
            search = true,
            color = {"admin:Green"},
            name = {"用户名称", "en:Username"})
    public String id;

    @ModelField(
            group = "base",
            list = true, search = true,
            pattern = "^\\+?[1-9]\\d{1,14}$",
            name = {"手机号码", "en:Mobile Phone Number"})
    public String phoneNumber;

    @ModelField(
            group = "base",
            required = true,
            input_type = InputType.radio, echo_group = "aa",
            list = true, search = true,
            name = {"用户性别", "en:User Gender"})
    public String gender;

    @ModelField(
            group = "org",
            input_type = InputType.select,
            reference = Post.class,
            list = true, search = true,
            update = true,
            name = {"岗位", "en:Position"})
    public String position;

    @ModelField(
            group = "org",
            input_type = InputType.multiselect,
            reference = Department.class,
            list = true, search = true,
            update = true,
            name = {"归属部门", "en:Department "})
    public String department;

    @ModelField(
            input_type = InputType.sortable,
            list = true,
            separator = "@",
            name = {"项目1", "en:1"})
    public String subjects1;

    @ModelField(
            input_type = InputType.checkbox,
            separator = "@",
            reference = Post.class,
            name = {"checkbox", "en:1"})
    public String checkbox;

    @ModelField(
            input_type = InputType.kv,
            separator = "@",
            name = {"kv", "en:1"})
    public String kv;

    @ModelField(
            input_type = InputType.sortable_checkbox,
            separator = "#",
            name = {"项目2", "en:2"})
    public String subjects2;

    @ModelField(
            input_type = InputType.sortable_checkbox,
            separator = "#",
            name = {"项目3", "en:3"})
    public String subjects3;

    @ModelField(
            readonly = true,
            name = {"创建后不可编辑", "en:"})
    public String noEdit;

    @ModelField(
            input_type = InputType.textarea,
            list = true, search = true,
            skip = {">", "("},
            name = {"备注", "en:Notes"})
    public String notes = "只读控制";

    @ModelAction(
            code = "test", icon = "circle-arrow-up",
            link_fields = {"name", "notes", "gender", "a", "b"},
            action_type = ActionType.PopLayer,
            name = {"弹出表单", "en:test"},
            info = {"弹出表单", "en:test"})
    public void test(Request request) {
        request.getResponse().useCustomizedResponse(
                String.format("处理参数，name：%s，gender：%s",
                        request.getParameter("name"),
                        request.getParameter("gender")));
    }

    @Override
    public String[] listActions() {
        return new String[]{"subTab", "test", "edit"};
    }

    @Override
    public boolean showOrderNumber() {
        return false;
    }

    @ModelAction(
            code = "share", icon = "share-alt",
            redirect = "list",
            name = {"头部按钮", "en:Share"},
            info = {"头部按钮", "en:Share"})
    public void share(Request request) {
        System.out.println("点击了头部按钮。。。");
    }

    @Override
    public String[] staticOptionFields() {
        return new String[]{"gender", "checkbox"};
    }

    @Override
    public String[] dynamicOptionFields() {
        return new String[]{"subjects2", "subjects3"};
    }

    @Override
    public Item[] optionData(String fieldName) {
        switch (fieldName) {
            case "gender":
                return new Item[]{
                        Item.of("0", new String[]{"男", "en:man"}),
                        Item.of("1", new String[]{"女", "en:woman"})
                };
            case "checkbox":
                return Item.of(new String[]{
                        "java", "python", "js"
                });
            case "subjects2":
                return new Item[]{
                        Item.of("1", new String[]{"一", "en:One"}),
                        Item.of("2", new String[]{"二", "en:Two"}),
                        Item.of("3", new String[]{"三", "en:Three"})
                };
            case "subjects3":
                return Item.of(new String[]{"a", "b", "c", "d", "e"});
            case "position": // 没有设置静态和动态选项字段，无效代码
                return Item.of(new String[]{"a", "b", "c"});
        }
        return null;
    }

    @Override
    public Item[] groupData() {
        return new Item[]{
                Item.of("base", new String[]{"基本信息", "en:Base"}),
                Item.of("org", new String[]{"组织关系", "en:Org"})
        };
    }

    @Override
    public Map<String, String> echoData(String echoGroup, Map<String, String> params) {
        if (echoGroup.equals("aa")) {
            Map<String, String> map = new HashMap<>();
            if (params.get("gender").equals("0")) {
                map.put("position", "001");
                map.put("department", "一部");
                map.put("subjects1", "123," + params.get("gender"));
                map.put("checkbox", "python");
                map.put("subjects2", "3,2");
                map.put("kv", "a=123@b=addf");
            } else {
                map.put("position", "002");
                map.put("department", "二部");
                map.put("subjects1", params.get("gender"));
                map.put("checkbox", "js");
                map.put("subjects2", "1,2");
                map.put("kv", "hello=world@lang=");
            }
            map.put("notes", params.get("gender"));

            return map;
        } else {
            return Collections.emptyMap();
        }
    }

    @Override
    public Map<String, String> defaultSearch() {
        return new HashMap<String, String>() {{
            put("gender", "1");
        }};
    }
}
