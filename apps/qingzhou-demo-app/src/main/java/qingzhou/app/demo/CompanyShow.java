package qingzhou.app.demo;

import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.type.Show;

import java.util.HashMap;
import java.util.Map;

/**
 * 演示菜单点击直接进入Show详情页的Model
 * 通过设置 @Model 的 action = "show" 实现
 */
@Model(code = "company-show", order = 10,
        name = {"公司信息", "en:Company Info(Show Direct)"},
        info = {"演示菜单点击直接进入Show详情页", "en:Demo menu click to show page directly"},
        icon = "OfficeBuilding",
        menu = "advanced",
        action = "show")  // 关键：设置默认action为show
public class CompanyShow extends qingzhou.api.ModelBase implements Show {

    // 模拟单条数据，show类型只展示一条记录
    private final Map<String, String> companyData = new HashMap<>();

    public CompanyShow() {
        // 初始化公司数据
        companyData.put("id", "COMP001");
        companyData.put("name", "轻舟科技有限公司");
        companyData.put("code", "QINGZHOU");
        companyData.put("industry", "互联网/软件开发");
        companyData.put("scale", "100-500人");
        companyData.put("address", "北京市海淀区中关村科技园");
        companyData.put("phone", "010-12345678");
        companyData.put("email", "contact@qingzhou.com");
        companyData.put("website", "www.qingzhou.com");
        companyData.put("founder", "张三");
        companyData.put("foundedAt", "2020-01-15");
        companyData.put("description", "轻舟科技是一家专注于云原生应用平台研发的高科技企业，致力于为企业提供一站式的应用开发、部署和管理解决方案。");
        companyData.put("mission", "让应用开发更简单，让数字化转型更高效");
        companyData.put("vision", "成为全球领先的云原生应用平台提供商");
    }

    @ModelField(id = true,
            name = {"企业ID", "en:Company ID"},
            info = {"企业唯一标识", "en:Company unique identifier"},
            show = true,
            readonly = true)
    public String id;

    @ModelField(
            name = {"企业名称", "en:Company Name"},
            info = {"企业全称", "en:Full company name"},
            show = true,
            group = {"基本信息", "en:Basic Info"})
    public String name;

    @ModelField(
            name = {"企业代码", "en:Company Code"},
            info = {"企业代码/简称", "en:Company code"},
            show = true,
            group = {"基本信息", "en:Basic Info"})
    public String code;

    @ModelField(
            name = {"所属行业", "en:Industry"},
            info = {"所属行业领域", "en:Industry sector"},
            show = true,
            group = {"基本信息", "en:Basic Info"})
    public String industry;

    @ModelField(
            name = {"企业规模", "en:Scale"},
            info = {"企业人员规模", "en:Company scale"},
            show = true,
            group = {"基本信息", "en:Basic Info"})
    public String scale;

    @ModelField(
            name = {"企业地址", "en:Address"},
            info = {"企业注册地址", "en:Registered address"},
            show = true,
            group = {"联系信息", "en:Contact Info"})
    public String address;

    @ModelField(
            name = {"联系电话", "en:Phone"},
            info = {"企业联系电话", "en:Contact phone"},
            show = true,
            group = {"联系信息", "en:Contact Info"})
    public String phone;

    @ModelField(
            name = {"企业邮箱", "en:Email"},
            info = {"企业官方邮箱", "en:Official email"},
            show = true,
            group = {"联系信息", "en:Contact Info"})
    public String email;

    @ModelField(
            name = {"官方网站", "en:Website"},
            info = {"企业官方网站", "en:Official website"},
            show = true,
            group = {"联系信息", "en:Contact Info"})
    public String website;

    @ModelField(
            name = {"创始人", "en:Founder"},
            info = {"企业创始人", "en:Company founder"},
            show = true,
            group = {"企业信息", "en:Company Info"})
    public String founder;

    @ModelField(
            name = {"成立时间", "en:Founded Date"},
            info = {"企业成立日期", "en:Date of establishment"},
            show = true,
            group = {"企业信息", "en:Company Info"})
    public String foundedAt;

    @ModelField(
            name = {"企业简介", "en:Description"},
            info = {"企业详细介绍", "en:Company description"},
            show = true,
            group = {"企业介绍", "en:About"})
    public String description;

    @ModelField(
            name = {"企业使命", "en:Mission"},
            info = {"企业使命宣言", "en:Company mission"},
            show = true,
            group = {"企业介绍", "en:About"})
    public String mission;

    @ModelField(
            name = {"企业愿景", "en:Vision"},
            info = {"企业愿景目标", "en:Company vision"},
            show = true,
            group = {"企业介绍", "en:About"})
    public String vision;

    public boolean contains(String id) {
        // 支持 default id 或实际的 company id
        return "default".equals(id) || "COMP001".equals(id);
    }

    @Override
    public Map<String, String> show(Request request) {
        // 返回固定的公司信息数据（无论id是什么，都返回同样的数据）
        return new HashMap<>(companyData);
    }
}
