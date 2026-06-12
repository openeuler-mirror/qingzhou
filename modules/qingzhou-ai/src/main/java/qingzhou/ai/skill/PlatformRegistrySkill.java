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
}
