package qingzhou.app.model;

import qingzhou.api.*;
import qingzhou.api.type.Delete;
import qingzhou.api.type.Echo;
import qingzhou.api.type.Group;
import qingzhou.api.type.Option;
import qingzhou.app.AddModelBase;
import qingzhou.app.ExampleMain;
import qingzhou.ssh.SSHClient;
import qingzhou.ssh.SSHResult;
import qingzhou.ssh.SSHService;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;


@Model(code = User.code, icon = "user",
        menu = ExampleMain.MENU_1, order = "1",
        name = {"用户", "en:User management"},
        info = {"用户管理", "en:User management."})
public class User extends AddModelBase implements Delete, Group, Option, Echo {
    public static final String code = "user-model-code";
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
            // ref_model = Post.class,
            list = true, search = true,
            // update_action = "update",
            name = {"岗位", "en:Position"},
            info = {"岗位", "en:Position"})
    public String position;

    @ModelField(
            group = "org",
            input_type = InputType.multiselect,
            ref_model = Department.class,
            list = true, search = true,
            update_action = "update",
            name = {"归属部门", "en:Department "})
    public String department;

    @ModelField(
            input_type = InputType.checkbox,
            separator = "@",
            ref_model = Post.class,
            name = {"checkbox", "en:1"})
    public String checkbox;

    @ModelField(
            input_type = InputType.kv,
            separator = "@",
            name = {"kv", "en:1"})
    public String kv;

    @ModelField(
            input_type = InputType.sortable_checkbox,
            separator = "#", order = "2",
            name = {"项目2", "en:2"})
    public String subjects2;

    @ModelField(
            input_type = InputType.sortable_checkbox,
            separator = "#", order = "3",
            name = {"项目3", "en:3"})
    public String subjects3;

    @ModelField(
            input_type = InputType.sortable,
            list = true, order = "1",
            separator = "@",
            name = {"项目1", "en:1"})
    public String subjects1;

    @ModelField(
            plain_text = true,
            name = {"创建后不可编辑", "en:"})
    public String noEdit;

    @ModelField(
            input_type = InputType.textarea,
            skip = {">", "("},
            name = {"备注", "en:Notes"})
    public String notes = "只读控制";

    @ModelField(
            input_type = InputType.select, create = false, edit = false,
            name = {"字段", "en:field"})
    public String field;

    @ModelField(
            input_type = InputType.select, create = false, edit = false,
            name = {"操作符", "en:operator"})
    public String operator;

    @ModelField(
            create = false, edit = false,
            name = {"值", "en:value"})
    public String value;

    @ModelField(
            create = false, edit = false,
            input_type = InputType.bool,
            name = {"自定义标签", "en:Custom labels"})
    public boolean customLabel;

    @ModelField(
            input_type = InputType.combine,
            combine_fields = {"field", "operator", "value", "customLabel"},
            skip = {"[", "]", "\"", ">", "<", "'"},
            search = true,
            name = {"组合查询", "en:comboQuery"},
            info = {"添加过滤条件", "en:Add filters"})
    public String comboQuery;

    @ModelField(
            input_type = InputType.combine,
            combine_fields = {"field", "operator", "value", "customLabel"},
            skip = {"[", "]", "\"", ">", "<", "'"},
            search = true,
            name = {"组合查询2", "en:comboQuery2"},
            info = {"添加过滤条件2", "en:Add filters2"})
    public String comboQuery2;

    @Override
    public boolean showOrderNumber() {
        return false;
    }

    @ModelAction(
            code = "share", icon = "share-alt",
            head_action = true, order = "3",
            action_type = ActionType.action_list,
            name = {"头部按钮", "en:Share"},
            info = {"头部按钮", "en:Share"})
    public void share(Request request) {
        System.out.println("点击了头部按钮。。。");
    }

    @ModelAction(
            code = "test", icon = "circle-arrow-up",
            head_action = true, list_action = true, order = "2",
            sub_form_fields = {"id", "gender", "position", "checkbox", "notes", "b"},
            action_type = ActionType.sub_form,
            sub_form_autoload = false,
            sub_form_autoclose = true,
            name = {"弹出表单", "en:test"},
            info = {"弹出表单", "en:test"})
    public void test(Request request) throws Exception {
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
                if (contains(request.getParameter("id"))) {
                    updateData(map);
                } else {
                    addData(map);
                }
                request.getResponse().setData(map);
            } else if ("python".equals(checkbox)) {
                request.getResponse().setSuccess(false);
                request.getResponse().setMsg("处理异常");
            } else if ("js".equals(checkbox)) {
                request.getResponse().setMsg("处理完成！");
            }
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
            request.getResponse().setData(html);
        }
    }

    @ModelAction(
            code = "upload", icon = "upload-alt",
            head_action = true,
            order = "4",
            action_type = ActionType.upload,
            name = {"上传", "en:upload"},
            info = {"将本地文件或数据发送到服务器进行存储和处理。",
                    "en:Send local files or data to a server for storage and processing."})
    public void upload(Request request) throws Exception {
        System.out.println("文件已上传至临时目录：" + request.getParameter("upload"));
    }

    @ModelAction(
            code = "upload1", icon = "upload-alt",
            head_action = true, order = "41",
            action_type = ActionType.upload,
            name = {"上传1", "en:upload1"},
            info = {"将本地文件或数据发送到服务器进行存储和处理。",
                    "en:Send local files or data to a server for storage and processing."})
    public void upload1(Request request) throws Exception {
        System.out.println("文件1已上传至临时目录：" + request.getParameter("upload1"));
    }


    @ModelField(field_type = FieldType.OTHER,
            host = true,
            required = true,
            name = {"主机名", "en:hostname"})
    public String hostname;
    @ModelField(field_type = FieldType.OTHER,
            port = true,
            required = true,
            name = {"端口", "en:port"})
    public int port = 22;
    @ModelField(field_type = FieldType.OTHER,
            required = true,
            name = {"用户名", "en:username"})
    public String username;
    @ModelField(field_type = FieldType.OTHER,
            input_type = InputType.password,
            required = true,
            name = {"密码", "en:password"})
    public String password;
    @ModelField(field_type = FieldType.OTHER,
            required = true,
            name = {"命令", "en:command"})
    public String command;

    @ModelAction(code = "ssh", icon = "code",
            head_action = true, action_type = ActionType.sub_form,
            sub_form_fields = {"hostname", "port", "username", "password", "command"},
            name = {"SSH 测试", "en: SSH Test"})
    public void ssh(Request request) throws Exception {
        Object service; // 没有安装时，类会找不到，故使用 Object 类型
        try {
            service = getAppContext().getService(SSHService.class);
        } catch (Throwable e) {
            request.getResponse().setSuccess(false);
            request.getResponse().setMsg("The   SSH service is not installed: " + e.getMessage());
            return;
        }

        SSHService ssh = (SSHService) service;
        User user = request.getParameterAsObject(User.class);
        SSHClient sshClient = ssh.createSSHClientBuilder()
                .host(user.hostname)
                .port(user.port)
                .username(user.username)
                .password(user.password).build();
        SSHResult result = sshClient.execCmd(user.command);
        request.getResponse().setSuccess(result.isSuccess());
        request.getResponse().setContentType("text/html;charset=UTF-8");
        request.getResponse().setData(result.getMessage());
    }

    @Override
    public String[] staticOptionFields() {
        return new String[]{"gender", "checkbox", "position", "field", "operator", "customLabel"};
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
            case "field":
                return Item.of(new String[]{"gender", "position", "checkbox"});
            case "operator":
                return Item.of(new String[]{">", "=", "<", "<=", "!=", "in", "not in", "like", "not like", "is null", "is not null"});
            case "customLabel":
                return Item.of(new String[]{"true", "false"});
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
    public void echoData(String echoGroup, Map<String, String> params, DataBuilder dataBuilder) {
        if ("aa".equals(echoGroup)) {
            if ("0".equals(params.get("gender"))) {
                dataBuilder.addData("position", "003");
                dataBuilder.addData("department", "一部");
                dataBuilder.addData("subjects1", "123," + params.get("gender"));
                dataBuilder.addData("checkbox", "python");
                dataBuilder.addData("subjects2", "3,2");
                dataBuilder.addData("kv", "a=123@b=addf");
            } else {
                dataBuilder.addData("position", "002");
                dataBuilder.addData("department", "二部");
                dataBuilder.addData("subjects1", params.get("gender"));
                dataBuilder.addData("checkbox", "js");
                dataBuilder.addData("subjects2", "1,2");
                dataBuilder.addData("kv", "hello=world@lang=");
            }
            dataBuilder.addData("notes", params.get("gender"));
        }
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
    public int pageSize() {
        return 1;
    }
}
