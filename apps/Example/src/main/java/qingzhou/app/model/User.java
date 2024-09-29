package qingzhou.app.model;

import qingzhou.api.*;
import qingzhou.app.AddModelBase;
import qingzhou.app.ExampleMain;

import java.util.HashMap;
import java.util.Map;


@Model(code = "user", icon = "user",
        menu = ExampleMain.SYSTEM_MANAGEMENT, order = 1,
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
            group = "职位",
            type= FieldType.select,
            refModel = Post.class,
            list = true,
            name = {"岗位", "en:Position"})
    public String position;

    @ModelField(
            type = FieldType.select,
            options = {"a", "b", "c", "d"},
            // refModel = Department.class,
            list = true,
            name = {"归属部门", "en:Department "})
    public String department;

    @ModelField(
            show = "department=a",
            name = {"test", "en:test"})
    public String test;

    @ModelField(
            type = FieldType.textarea,
            list = true,
            linkModel = "department.email",
            name = {"备注", "en:Notes"})
    public String notes;

    @Override
    public String idField() {
        return "name";
    }

    @ModelAction(
            code = "test", icon = "trash",
            order = 9,
            showFields = {"name", "phoneNumber", "sex", "department", "test", "notes"},
            name = {"测试", "en:Test"},
            info = {"测试自定义action。",
                    "en:Test custom action."})
    public void test(Request request) {
        String id = request.getId();
        Response response = request.getResponse();
        response.setSuccess(true);
        response.setSuccess(false);
        response.setMsg("ssasa");
        Map<String, String> map = new HashMap<>();
        map.put("name", "test");
        map.put("id", id);
        response.addData(map);
    }

    @ModelAction(
            code = "test1", icon = "trash",
            order = 9,
            showFields = {"position", "phoneNumber", "sex"},
            name = {"测试1", "en:Test1"},
            info = {"测试自定义action。",
                    "en:Test custom action."})
    public void test1(Request request) {
        String id = request.getId();
        System.out.println(id);
    }


}
