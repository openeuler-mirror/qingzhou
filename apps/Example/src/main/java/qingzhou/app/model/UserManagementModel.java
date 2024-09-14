package qingzhou.app.model;

import qingzhou.api.*;
import qingzhou.app.ExampleMain;

import qingzhou.app.AddableModelBase;


@Model(code = "usermanagement", icon = "user",
        menu = ExampleMain.SYSTEM_MANAGEMENT, order = 3,
        name = {"用户管理", "en:User management"},
        info = {"用户管理", "en:User management."})
public class UserManagementModel extends AddableModelBase {
    @ModelField(
            required = true,
            list = true,
            name = {"用户名称", "en:Username"},
            info = {"用户名称。", "en:Username."})
    public String name;

    @ModelField(
            required = true,
            list = true,
            pattern = "^\\+?[1-9]\\d{1,14}$",
            name = {"手机号码", "en:Mobile Phone Number"},
            info = {"手机号码。", "en:Mobile phone number."})
    public String phoneNumber;

    @ModelField(
            type = FieldType.radio,
            required = true,
            options = {"男", "女"},
            list = true,
            name = {"用户性别", "en:User Gender"},
            info = {"用户性别。", "en:User gender."})
    public String sex;

    @ModelField(
            type = FieldType.select,
            refModel = PostManagementModel.class,
            required = true,
            list = true,
            name = {"岗位", "en:Position"},
            info = {"岗位。", "en:Position."})
    public String position;

    @ModelField(
            type = FieldType.select,
            refModel = DepartmentModel.class,
            list = true,
            name = {"归属部门", "en:Department "},
            info = {"归属部门。", "en:Department ."})
    public String department;

    @ModelField(
            type = FieldType.textarea,
            list = true,
            name = {"备注", "en:Notes"},
            info = {"备注。", "en:Notes."})
    public String notes;

    @Override
    public String idFieldName() {
        return "name";
    }
}
