package qingzhou.ai.skill;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import qingzhou.ai.AiSkill;
import qingzhou.ai.AiTool;
import qingzhou.ai.SkillName;

@Component(property = {
        AiSkill.SKILL_NAME + "=" + SkillName.PLATFORM_REGISTRY,
        AiSkill.SKILL_DESCRIPTION + "=提供平台上应用注册信息的查询检索。"
})
public class PlatformRegistrySkill extends SkillBase implements AiSkill {
    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE,
            target = "(" + AiTool.TOOL_SKILL_NAME + "=" + SkillName.PLATFORM_REGISTRY + ")", // 按服务属性过滤
            unbind = "unbindAiTool") // 定义在：SkillBase.unbindAiTool
    public void bindAiTool(AiTool tool, Map<String, Object> properties) {
        aiTools.put(tool, properties);
    }

    // OSGI 框架根据名称规则自动识别调用此方法或在子类的 @Reference 中指定
    public void unbindAiTool(AiTool tool) {
        aiTools.remove(tool);
    }

    @Override
    public String getInstruction() {
        return "# 平台应用信息检索技能\n" +
                "\n" +
                "## 技能定位\n" +
                "\n" +
                "解答关于轻舟平台“有哪些应用 → 应用有哪些功能 → 功能如何交互/展示”的问题。\n" +
                "\n" +
                "## 核心能力\n" +
                "\n" +
                "支持查询以下三层元数据结构：\n" +
                "\n" +
                "### 1. 应用列表\n" +
                "\n" +
                "- **用途**：获取平台上所有可访问的应用概览\n" +
                "- **信息项**：标识、名称、地址、描述\n" +
                "\n" +
                "### 2. 应用详情\n" +
                "\n" +
                "- **用途**：获取指定应用的完整结构\n" +
                "- **信息项**：基本信息 + 功能模块清单 + 菜单层级体系\n" +
                "\n" +
                "### 3. 模块元数据\n" +
                "\n" +
                "- **用途**：获取指定功能模块的交互与数据定义\n" +
                "- **包含内容**：\n" +
                "\n" +
                "| 维度   | 说明                             |\n" +
                "|------|--------------------------------|\n" +
                "| 字段定义 | 数据类型、校验规则、输入方式、展示规则（表单/列表可见性等） |\n" +
                "| 操作定义 | 支持的操作行为（增删改查等）、可用场景（行级/批量/头部等） |\n";
    }
}
