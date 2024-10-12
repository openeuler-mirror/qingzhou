package qingzhou.app.model;

import qingzhou.api.*;
import qingzhou.api.type.Group;
import qingzhou.api.type.Option;
import qingzhou.app.AddModelBase;
import qingzhou.app.ExampleMain;


@Model(code = "user", icon = "user",
        menu = ExampleMain.MENU_1, order = 1,
        name = {"用户", "en:User management"},
        info = {"用户管理", "en:User management."})
public class User extends AddModelBase implements Group, Option {
    @ModelField(
            group = "base",
            required = true,
            list = true,
            color = {"admin:#66de65"},
            name = {"用户名称", "en:Username"})
    public String name;

    @ModelField(
            group = "base",
            required = true,
            list = true,
            pattern = "^\\+?[1-9]\\d{1,14}$",
            name = {"手机号码", "en:Mobile Phone Number"})
    public String phoneNumber;

    @ModelField(
            group = "base",
            type = FieldType.radio,
            required = true,
            list = true,
            name = {"用户性别", "en:User Gender"})
    public String sex;

    @ModelField(
            group = "org",
            type = FieldType.select,
            refModel = Post.class,
            list = true, search = false,
            name = {"岗位", "en:Position"})
    public String position;

    @ModelField(
            group = "org",
            type = FieldType.select,
            refModel = Department.class,
            list = true,
            name = {"归属部门", "en:Department "})
    public String department;

    @ModelField(
            type = FieldType.sortable,
            list = true,
            separator = "@",
            name = {"项目1", "en:1"})
    public String subjects1;

    @ModelField(
            type = FieldType.checkbox,
            list = true,
            separator = "@",
            refModel = Post.class,
            name = {"checkbox", "en:1"})
    public String checkbox;
    @ModelField(
            type = FieldType.multiselect,
            list = true,
            separator = "@",
            refModel = Post.class,
            name = {"multiselect", "en:1"})
    public String multiselect;
    @ModelField(
            type = FieldType.kv,
            list = true,
            separator = "@",
            name = {"kv", "en:1"})
    public String kv;

    @ModelField(
            type = FieldType.sortablecheckbox,
            list = true,
            separator = "#",
            name = {"项目2", "en:2"})
    public String subjects2;

    @ModelField(
            type = FieldType.sortablecheckbox,
            list = true,
            separator = "#",
            name = {"项目3", "en:3"})
    public String subjects3;

    @ModelField(
            type = FieldType.textarea,
            list = true, search = false,
            link = "department.email",
            name = {"备注", "en:Notes"})
    public String notes;

    @ModelAction(
            code = "test", icon = "circle-arrow-up",
            list = true,
            head = true, order = 1,
            fields = {"name", "notes", "sex", "a", "b"},
            name = {"弹出表单", "en:test"},
            info = {"弹出表单", "en:test"})
    public void test(Request request) {
        System.out.println(request.getParameterNames());
    }

    @Override
    public String idField() {
        return "name";
    }

    @ModelAction(
            code = "share", icon = "share-alt",
            page = "list",
            head = true, order = 2,
            name = {"头部按钮", "en:Share"},
            info = {"头部按钮", "en:Share"})
    public void share(Request request) {
        System.out.println("点击了头部按钮。。。");
    }

    @Override
    public Item[] optionData(String fieldName) {
        switch (fieldName) {
            case "sex":
                return new Item[]{
                        Item.of("0", new String[]{"男", "en:man"}),
                        Item.of("1", new String[]{"女", "en:woman"})
                };
            case "subjects2":
                return new Item[]{
                        Item.of("1", new String[]{"一", "en:One"}),
                        Item.of("2", new String[]{"二", "en:Two"}),
                        Item.of("3", new String[]{"三", "en:Three"})
                };
            case "subjects3":
                return Item.of(new String[]{"a", "b", "c", "d", "e"});
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
}
