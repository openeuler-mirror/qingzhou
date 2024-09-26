package qingzhou.app.model;

import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.app.AddableModelBase;
import qingzhou.app.ExampleMain;


@Model(code = "user", icon = "user",
        menu = ExampleMain.SYSTEM_MANAGEMENT, order = 1,
        name = {"用户", "en:User management"},
        info = {"用户管理", "en:User management."})
public class User extends AddableModelBase {
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
            refModel = Post.class,
            list = true,
            name = {"岗位", "en:Position"},
            info = {"岗位。", "en:Position."})
    public String position;

    @ModelField(
            type = FieldType.select,
            refModel = Department.class,
            list = true,
            name = {"归属部门", "en:Department "},
            info = {"归属部门。", "en:Department ."})
    public String department;

    @ModelField(
            type = FieldType.textarea,
            list = true,
            linkModel = "department.email",
            name = {"备注", "en:Notes"},
            info = {"备注。", "en:Notes."})
    public String notes;

    @Override
    public String idFieldName() {
        return "name";
    }
}
