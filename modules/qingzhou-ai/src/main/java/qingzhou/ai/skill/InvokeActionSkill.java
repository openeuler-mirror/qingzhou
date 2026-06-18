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
        AiSkill.SKILL_NAME + "=" + SkillName.ACTION_INVOKER,
        AiSkill.SKILL_DESCRIPTION + "=执行应用的操作，获取应用模块的数据信息。"
})
public class InvokeActionSkill extends SkillBase implements AiSkill {
    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE,
            target = "(" + AiTool.TOOL_SKILL_NAME + "=" + SkillName.ACTION_INVOKER + ")", // 按服务属性过滤
            unbind = "unbindAiTool") // 定义在：SkillBase.unbindAiTool
    public void bindAiTool(AiTool tool, Map<String, Object> properties) {
        aiTools.put(tool, properties);
    }

    // OSGI 框架根据名称规则自动识别调用此方法或在子类的 @Reference 中指定
    public void unbindAiTool(AiTool tool) {
        aiTools.remove(tool);
    }

    @Override
    public String[] getI18n() {
        return new String[]{"应用数据管理专家", "en:App Data Expert"};
    }

    @Override
    public String getInstruction() {
        return "# 执行应用操作\n" +
                "\n" +
                "对应用模块内的业务数据执行操作或查询状态。\n" +
                "\n" +
                "## 核心能力\n" +
                "\n" +
                "### 1. 执行与查询\n" +
                "\n" +
                "触发模块操作，获取业务数据（如列表）。\n" +
                "\n" +
                "### 2. 数据详情\n" +
                "\n" +
                "获取指定业务数据的完整信息。\n" +
                "\n" +
                "### 3. 状态监控\n" +
                "\n" +
                "获取模块或数据的实时变量（健康、告警等）。\n" +
                "\n" +
                "### 4. 操作校验\n" +
                "\n" +
                "轻量判断模块是否支持某操作（返回是/否，不拉取完整详情）。\n";
    }
}
