package qingzhou.app.model;

import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.app.AddModelBase;

@Model(code = "department", icon = "sitemap",
        menu = qingzhou.app.ExampleMain.MENU_11, order = 1,
        name = {"部门", "en:Department"},
        info = {"对系统中的部门进行管理，以方便项目登录人员的管理。", "en:Manage departments in the system to facilitate the management of project logged in personnel."})
public class Department extends AddModelBase {
    @ModelField(
            required = true,
            list = true,
            name = {"部门名称", "en:Department Name"},
            info = {"该部门的详细名称。", "en:The name of the department."})
    public String name;

    @ModelField(
            list = true,
            name = {"上级部门", "en:Superior Department"},
            info = {"该部门所属的上级部门。",
                    "en:The superior department to which the department belongs."})
    public String superior = "";

    @ModelField(
            list = true,
            name = {"负责人", "en:Department Manager"},
            info = {"该部门的负责人姓名。", "en:Name of the head of the department."})
    public String manager;

    @ModelField(
            list = true,
            name = {"联系电话", "en:Department Phone"},
            info = {"该部门的联系电话。", "en:The department contact number."})
    public String phone;

    @ModelField(
            list = true,
            email = true,
            name = {"电子邮箱", "en:Department Email"},
            info = {"可以与该部门取得联系的电子邮箱。", "en:An E-mail address where the department can be contacted."})
    public String email;

    @Override
    public String idField() {
        return "name";
    }
}
