package qingzhou.app.system.setting;

import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.type.Addable;
import qingzhou.app.system.Main;
import qingzhou.config.Config;
import qingzhou.config.Console;
import qingzhou.engine.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Model(code = "department", icon = "sitemap",
        menu = Main.SETTING_MENU, order = 3,
        name = {"部门", "en:Department"},
        info = {"对系统中的部门进行管理，以方便项目登录人员的管理。", "en:Manage departments in the system to facilitate the management of project logged in personnel."})
public class Department extends ModelBase implements Addable {

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
    public void addData(Map<String, String> data) throws Exception {
        qingzhou.config.Department d = new qingzhou.config.Department();
        Utils.setPropertiesToObj(d, data);
        Main.getService(Config.class).addDepartment(d);
    }

    @Override
    public void deleteData(String id) throws Exception {
        Main.getService(Config.class).deleteDepartment(id);
    }

    @Override
    public String idFieldName() {
        return "name";
    }

    @Override
    public List<Map<String, String>> listData(int pageNum, int pageSize, String[] fieldNames) throws Exception {
        List<Map<String, String>> departments = new ArrayList<>();
        Console console = Main.getService(Config.class).getConsole();
        for (qingzhou.config.Department department : console.getDepartment()) {
            Map<String, String> departmentMap = Utils.getPropertiesFromObj(department);
            departments.add(departmentMap);
        }
        return departments;
    }

    @Override
    public int totalSize() {
        Console console = Main.getService(Config.class).getConsole();
        qingzhou.config.Department[] department = console.getDepartment();
        return department != null ? department.length : 0;
    }

    @Override
    public Map<String, String> showData(String id) throws Exception {
        Console console = Main.getService(Config.class).getConsole();
        for (qingzhou.config.Department department : console.getDepartment()) {
            if (department.getName().equals(id)) return Utils.getPropertiesFromObj(department);
        }
        return null;
    }

    @Override
    public void updateData(Map<String, String> data) throws Exception {
        Config config = Main.getService(Config.class);
        String id = data.get(idFieldName());
        qingzhou.config.Department department = config.getConsole().getDepartment(id);
        config.deleteDepartment(id);
        Utils.setPropertiesToObj(department, data);
        config.addDepartment(department);
    }
}