package qingzhou.app.model;

import qingzhou.api.*;
import qingzhou.api.type.Add;
import qingzhou.api.type.Echo;
import qingzhou.api.type.Group;
import qingzhou.api.type.Option;
import qingzhou.app.AddModelBase;
import qingzhou.app.ExampleMain;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;


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
            input_type = InputType.select, skip_validate = true,
            // reference = Post.class,
            list = true, search = true,
            update_action = "update",
            name = {"岗位", "en:Position"})
    public String position;

    @ModelField(
            group = "org",
            input_type = InputType.multiselect,
            reference = Department.class,
            list = true, search = true,
            update_action = "update",
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
            separator = "@", list = true,
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
            plain_text = true,
            name = {"创建后不可编辑", "en:"})
    public String noEdit;

    @ModelField(
            input_type = InputType.textarea,
            list = true,
            skip = {">", "("},
            name = {"备注", "en:Notes"})
    public String notes = "只读控制";

    @ModelAction(
            code = "test", icon = "circle-arrow-up",
            form_fields = {"name", "notes", "gender", "checkbox", "b"},
            action_type = ActionType.sub_form, sub_form_submit_on_open = true,
            name = {"弹出表单", "en:test"},
            info = {"弹出表单", "en:test"})
    public void test(Request request) {
        String gender = request.getParameter("gender");
        if (!"1".equals(gender)) {
            String checkbox = request.getParameter("checkbox");
            HashMap<String, String> map = new HashMap<>();
            if ("java".equals(checkbox)) {
                Enumeration<String> names = request.getParameterNames();
                while (names.hasMoreElements()) {
                    String key = names.nextElement();
                    String value = request.getParameter(key);
                    map.put(key, value);
                }
            } else if ("python".equals(checkbox)) {
                map.put("success", "false");
                map.put("msg", "处理异常");
            } else if ("js".equals(checkbox)) {
                map.put("success", "true");
                map.put("msg", "处理完成！");
            }
            request.getResponse().useCustomizedResponse(map);
        } else {
            request.getResponse().setContentType("text/html;charset=UTF-8");
            String html = "<div style='background-color: #fff;color: #333;padding: 10px;'>" +
                    "<table style='width: 100%'>" +
                    "<thead><tr style='height: 20px'>" +
                    "<th style='width: 25%'>用户名称</th>" +
                    "<th style='width: 25%'>用户性别</th>" +
                    "<th style='width: 50%'>备注</th>" +
                    "</tr></thead>" +
                    "<tbody><tr>" +
                    "<td>" + request.getParameter("id") + "</td>"
                    + "<td>" + request.getParameter("gender") + "</td>" +
                    "<td>" + request.getParameter("notes") + "</td>" +
                    "</tr></tbody>" +
                    "</table>" +
                    "</div>";
            request.getResponse().useCustomizedResponse(html);
        }
    }

    @Override
    public String[] listActions() {
        return new String[]{"test", "edit"};
    }

    @Override
    public boolean showOrderNumber() {
        return false;
    }

    @ModelAction(
            code = "share", icon = "share-alt",
            action_type = ActionType.action_list,
            name = {"头部按钮", "en:Share"},
            info = {"头部按钮", "en:Share"})
    public void share(Request request) {
        System.out.println("点击了头部按钮。。。");
    }

    @ModelAction(
            code = "upload", icon = "upload-alt",
            action_type = ActionType.upload,
            name = {"上传", "en:upload"},
            info = {"将本地文件或数据发送到服务器进行存储和处理。",
                    "en:Send local files or data to a server for storage and processing."})
    public void upload(Request request) throws Exception {
        System.out.println("文件已上传至临时目录：" + request.getParameter("upload"));
    }

    @ModelAction(
            code = "upload1", icon = "upload-alt",
            action_type = ActionType.upload,
            name = {"上传1", "en:upload1"},
            info = {"将本地文件或数据发送到服务器进行存储和处理。",
                    "en:Send local files or data to a server for storage and processing."})
    public void upload1(Request request) throws Exception {
        System.out.println("文件1已上传至临时目录：" + request.getParameter("upload1"));
    }

    @Override
    public String[] staticOptionFields() {
        return new String[]{"gender", "checkbox", "position"};
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
                return new Item[]{
                        Item.of("001", new String[]{"开发", "en:Dev"}),
                        Item.of("002", new String[]{"测试", "en:Test"}),
                };
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
        if ("aa".equals(echoGroup)) {
            Map<String, String> map = new HashMap<>();
            if ("0".equals(params.get("gender"))) {
                map.put("position", "003");
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

    @Override
    public Map<String, String> showData(String id) {
        Map<String, String> map = super.showData(id);
        if ("1".equals(id)) {
            map.put("position", "003");
        }
        return map;
    }

    @Override
    public boolean useDynamicDefaultSearch() {
        return true;
    }

    @Override
    public String[] headActions() {
        return new String[]{Add.ACTION_CREATE, "share", "test", "upload", "upload1"};
    }
}
