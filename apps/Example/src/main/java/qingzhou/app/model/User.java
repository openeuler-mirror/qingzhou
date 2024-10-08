package qingzhou.app.model;

import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.app.AddModelBase;
import qingzhou.app.ExampleMain;


@Model(code = "user", icon = "user",
        menu = ExampleMain.MENU_1, order = 1,
        name = {"用户", "en:User management"},
        info = {"用户管理", "en:User management."})
public class User extends AddModelBase {
    @ModelField(
            required = true,
            list = true,
            color = {"admin:success"},
            name = {"用户名称", "en:Username"})
    public String name;

    @ModelField(
            required = true,
            list = true,
            pattern = "^\\+?[1-9]\\d{1,14}$",
            name = {"手机号码", "en:Mobile Phone Number"})
    public String phoneNumber;

    @ModelField(
            type = FieldType.radio,
            required = true,
            options = {"男", "女"},
            list = true,
            name = {"用户性别", "en:User Gender"})
    public String sex;

    @ModelField(
            type = FieldType.select,
            refModel = Post.class,
            list = true,
            name = {"岗位", "en:Position"})
    public String position;

    @ModelField(
            type = FieldType.select,
            refModel = Department.class,
            list = true,
            name = {"归属部门", "en:Department "})
    public String department;

    @ModelField(
            type = FieldType.textarea,
            list = true,
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
    public void test(Request request) throws Exception {
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
    public void share(Request request) throws Exception {
        System.out.println("点击了头部按钮。。。");
    }
}
