package qingzhou.app.master.system;

import qingzhou.api.*;
import qingzhou.api.type.Createable;
import qingzhou.api.type.Deletable;
import qingzhou.api.type.Listable;
import qingzhou.app.master.MasterApp;
import qingzhou.config.Config;
import qingzhou.config.Console;
import qingzhou.engine.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Model(code = "department", icon = "sitemap",
        menu = "System", order = 3,
        name = {"部门", "en:Department"},
        info = {"对系统中的部门进行管理，以方便项目登录人员的管理。", "en:Manage departments in the system to facilitate the management of project logged in personnel."})
public class Department extends ModelBase implements Createable {
    public static final String parentKey = "parent";
    public static final String parentNameKey = "parentName";

    @ModelField(
            list = true,
            name = {"部门标识符", "en:Department Name"},
            info = {"该部门在系统中的唯一标识符。", "en:Unique identifier of the department in the system."})
    public String id;

    @ModelField(
            list = true,
            name = {"部门名称", "en:Department Name"},
            info = {"该部门的详细名称。", "en:The name of the department."})
    public String name;

    @ModelField(
            required = false,
            name = {"上级部门标识符", "en:Parent Department"},
            info = {"该部门所属的上级部门标识符。", "en:The parent department to which the department belongs."})
    public String parent = "";

    @ModelField(
            list = true, required = false, editable = false, createable = false,
            name = {"上级部门", "en:Parent Department"},
            info = {"该部门所属的上级部门名称。", "en:The parent department to which the department belongs."})
    public String parentName = "";

    @ModelField(
            list = true,
            name = {"负责人", "en:Department Manager"},
            info = {"该部门的负责人姓名。", "en:Name of the head of the department."})
    public String manager;

    @ModelField(
            list = true, required = false,
            name = {"联系电话", "en:Department Contact Number"},
            info = {"该部门的联系电话。", "en:The department's contact number."})
    public String phone;

    @ModelField(
            list = true, required = false,
            name = {"电子邮箱", "en:Department Contact Email"},
            info = {"可以与该部门取得联系的电子邮箱。", "en:An E-mail address where the department can be contacted."})
    public String email;

    @Override
    public void start() {
        appContext.addI18n("department.parent.not.exist", new String[]{"您输入的上级部门不存在，请检查部门标识符是否输入正确", "en:The parent department you have entered does not exist, please check if the department identifier is entered correctly"});
        appContext.addI18n("email.incorrect", new String[]{"您输入的邮箱地址格式不正确，请检查后重新输入", "en:The e-mail address you have entered is not in the correct format, please check and re-enter it"});
    }

    @ModelAction(
            name = {"添加", "en:Add"},
            info = {"按配置要求创建一个模块。", "en:Create a module as configured."})
    public void add(Request request, Response response) throws Exception {
        if (getDataStore().exists(request.getParameter(Listable.FIELD_NAME_ID))) {
            response.setSuccess(false);
            response.setMsg(appContext.getI18n(request.getLang(), "validator.exist"));
            return;
        }

        Map<String, String> newDepartment = request.getParameters();
        String validate;
        for (String name : newDepartment.keySet()) {
            validate = validate(request, name);
            if (validate != null) {
                response.setSuccess(false);
                response.setMsg(validate);
                return;
            }
        }

        getDataStore().addData(newDepartment.get(Listable.FIELD_NAME_ID), newDepartment);
    }

    @ModelAction(
            name = {"编辑", "en:Edit"},
            info = {"获得可编辑的数据或界面。", "en:Get editable data or interfaces."})
    public void edit(Request request, Response response) throws Exception {
        show(request, response);
    }

    @ModelAction(
            show = "id!=default",
            name = {"删除", "en:Delete"},
            info = {"删除这个组件，该组件引用的其它组件不会被删除。注：请谨慎操作，删除后不可恢复。", "en:Delete this component, other components referenced by this component will not be deleted. Note: Please operate with caution, it cannot be recovered after deletion."})
    public void delete(Request request, Response response) throws Exception {
        String id = request.getId();
        DataStore dataStore = getDataStore();
        dataStore.deleteDataById(id);
    }

    @ModelAction(name = {"查看", "en:Show"}, info = {"查看该组件的相关信息。", "en:View the information of this model."})
    public void show(Request request, Response response) throws Exception {
        DataStore dataStore = getDataStore();
        Map<String, String> data = dataStore.getDataById(request.getId());
        response.addData(data);
    }

    @ModelAction(
            name = {"更新", "en:Update"},
            info = {"更新这个模块的配置信息。", "en:Update the configuration information for this module."})
    public void update(Request request, Response response) throws Exception {
        Map<String, String> newDepartment = request.getParameters();
        String validate;
        for (String name : newDepartment.keySet()) {
            validate = validate(request, name);
            if (validate != null) {
                response.setSuccess(false);
                response.setMsg(validate);
                return;
            }
        }
        getDataStore().updateDataById(request.getId(), newDepartment);
    }

    private String validate(Request request, String fieldName) throws Exception {
        if (parentKey.equals(fieldName)) {
            String parentVal = request.getParameter(parentKey);
            if (!parentVal.isEmpty() && !getDataStore().exists(parentVal)) {
                return appContext.getI18n(request.getLang(), "department.parent.not.exist");
            }
        }

        if ("email".equals(fieldName)) {
            String emailVal = request.getParameter("email");
            Pattern regex = Pattern.compile("^(.+)@(\\S+)$");
            if (!emailVal.isEmpty() && !regex.matcher(emailVal).matches()) {
                return appContext.getI18n(request.getLang(), "email.incorrect");
            }
        }

        return null;
    }

    public static final DataStore DEPARTMENT_DATA_STORE = new DepartmentDataStore();

    @Override
    public DataStore getDataStore() {
        return DEPARTMENT_DATA_STORE;
    }

    private static class DepartmentDataStore implements DataStore {
        @Override
        public List<Map<String, String>> getAllData() throws Exception {
            List<Map<String, String>> departments = new ArrayList<>();
            Console console = MasterApp.getService(Config.class).getConsole();
            for (qingzhou.config.Department department : console.getDepartment()) {
                Map<String, String> departmentMap = Utils.getPropertiesFromObj(department);
                String parentName = "";
                if (!department.getParent().isEmpty()) {
                    parentName = console.getDepartment(department.getParent()).getName();
                }
                departmentMap.put(parentNameKey, parentName);
                departments.add(departmentMap);
            }
            return departments;
        }

        @Override
        public void addData(String id, Map<String, String> department) throws Exception {
            qingzhou.config.Department d = new qingzhou.config.Department();
            Utils.setPropertiesToObj(d, department);
            MasterApp.getService(Config.class).addDepartment(d);
        }

        @Override
        public void updateDataById(String id, Map<String, String> data) throws Exception {
            Config config = MasterApp.getService(Config.class);
            qingzhou.config.Department department = config.getConsole().getDepartment(id);
            config.deleteDepartment(id);
            Utils.setPropertiesToObj(department, data);
            config.addDepartment(department);
        }

        @Override
        public void deleteDataById(String id) throws Exception {
            MasterApp.getService(Config.class).deleteDepartment(id);
        }
    }
}
