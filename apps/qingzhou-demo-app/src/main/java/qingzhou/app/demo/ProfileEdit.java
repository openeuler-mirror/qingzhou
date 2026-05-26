package qingzhou.app.demo;

import qingzhou.api.InputType;
import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.type.Show;
import qingzhou.api.type.Update;

import java.util.HashMap;
import java.util.Map;

/**
 * 演示菜单点击直接进入Edit编辑页的Model
 * 通过实现 Update 和 Show 接口，前端根据优先级（list > edit > monitor > show）自动进入编辑页
 */
@Model(code = "profile-edit", order = 11,
        name = {"个人设置", "en:Profile Settings(Edit Direct)"},
        info = {"演示菜单点击直接进入Edit编辑页", "en:Demo menu click to edit page directly"},
        icon = "UserFilled",
        menu = "advanced")
public class ProfileEdit extends qingzhou.api.ModelBase implements Show, Update {

    // 模拟单条用户配置数据
    private final Map<String, String> profileData = new HashMap<>();

    public ProfileEdit() {
        // 初始化个人设置数据
        profileData.put("id", "USER001");
        profileData.put("username", "admin");
        profileData.put("nickname", "系统管理员");
        profileData.put("realName", "张三");
        profileData.put("email", "admin@qingzhou.com");
        profileData.put("phone", "13800138000");
        profileData.put("department", "技术部");
        profileData.put("position", "架构师");
        profileData.put("bio", "专注于云原生技术架构设计，热爱开源社区贡献。");
        profileData.put("language", "zh-CN");
        profileData.put("timezone", "Asia/Shanghai");
        profileData.put("theme", "auto");
        profileData.put("notifications", "true");
        profileData.put("emailAlert", "true");
        profileData.put("smsAlert", "false");
    }

    @ModelField(id = true,
            name = {"用户ID", "en:User ID"},
            info = {"用户唯一标识", "en:User unique identifier"},
            show = true,
            update = true,
            readonly = true)
    public String id;

    @ModelField(
            name = {"用户名", "en:Username"},
            info = {"登录用户名", "en:Login username"},
            show = true,
            update = true,
            readonly = true,
            group = {"基本信息", "en:Basic Info"})
    public String username;

    @ModelField(
            name = {"昵称", "en:Nickname"},
            info = {"显示昵称", "en:Display nickname"},
            show = true,
            update = true,
            required = true,
            group = {"基本信息", "en:Basic Info"})
    public String nickname;

    @ModelField(
            name = {"真实姓名", "en:Real Name"},
            info = {"真实姓名", "en:Real name"},
            show = true,
            update = true,
            group = {"基本信息", "en:Basic Info"})
    public String realName;

    @ModelField(
            name = {"邮箱", "en:Email"},
            info = {"电子邮箱地址", "en:Email address"},
            show = true,
            update = true,
            required = true,
            email = true,
            group = {"联系信息", "en:Contact Info"})
    public String email;

    @ModelField(
            name = {"手机号", "en:Phone"},
            info = {"手机号码", "en:Mobile phone number"},
            show = true,
            update = true,
            group = {"联系信息", "en:Contact Info"})
    public String phone;

    @ModelField(
            name = {"所属部门", "en:Department"},
            info = {"所属部门", "en:Department"},
            show = true,
            update = true,
            input_type = InputType.select,
            options = {"技术部", "产品部", "运营部", "市场部", "人事部", "财务部"},
            group = {"工作信息", "en:Work Info"})
    public String department;

    @ModelField(
            name = {"职位", "en:Position"},
            info = {"职位/岗位", "en:Job position"},
            show = true,
            update = true,
            group = {"工作信息", "en:Work Info"})
    public String position;

    @ModelField(
            name = {"个人简介", "en:Bio"},
            info = {"个人简介", "en:Personal biography"},
            show = true,
            update = true,
            input_type = InputType.textarea,
            group = {"个人介绍", "en:About"})
    public String bio;

    @ModelField(
            name = {"语言", "en:Language"},
            info = {"界面语言", "en:Interface language"},
            show = true,
            update = true,
            input_type = InputType.select,
            options = {"zh-CN", "en-US"},
            group = {"系统设置", "en:System Settings"})
    public String language;

    @ModelField(
            name = {"时区", "en:Timezone"},
            info = {"时区设置", "en:Timezone setting"},
            show = true,
            update = true,
            input_type = InputType.select,
            options = {"Asia/Shanghai", "Asia/Tokyo", "Europe/London", "America/New_York"},
            group = {"系统设置", "en:System Settings"})
    public String timezone;

    @ModelField(
            name = {"主题", "en:Theme"},
            info = {"界面主题", "en:Interface theme"},
            show = true,
            update = true,
            input_type = InputType.select,
            options = {"light", "dark", "auto"},
            group = {"系统设置", "en:System Settings"})
    public String theme;

    @ModelField(
            name = {"启用通知", "en:Enable Notifications"},
            info = {"是否启用系统通知", "en:Enable system notifications"},
            show = true,
            update = true,
            input_type = InputType.bool,
            group = {"通知设置", "en:Notification Settings"})
    public Boolean notifications;

    @ModelField(
            name = {"邮件提醒", "en:Email Alert"},
            info = {"是否接收邮件提醒", "en:Receive email alerts"},
            show = true,
            update = true,
            input_type = InputType.bool,
            group = {"通知设置", "en:Notification Settings"})
    public Boolean emailAlert;

    @ModelField(
            name = {"短信提醒", "en:SMS Alert"},
            info = {"是否接收短信提醒", "en:Receive SMS alerts"},
            show = true,
            update = true,
            input_type = InputType.bool,
            group = {"通知设置", "en:Notification Settings"})
    public Boolean smsAlert;

    public boolean contains(String id) {
        // 支持 default id 或任何 id（单条数据模式）
        return true;
    }

    @Override
    public Map<String, String> show(Request request) {
        // 返回个人设置数据（单条数据，不依赖id）
        return new HashMap<>(profileData);
    }

    @Override
    public void update(Request request, Map<String, String> data) throws Exception {
        // 更新个人设置数据
        profileData.putAll(data);
        profileData.put("id", "USER001");
    }
}
