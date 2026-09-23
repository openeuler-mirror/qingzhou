package qingzhou.registry.ai;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import qingzhou.ai.SkillService;
import qingzhou.ai.ToolService;

@Component(property = {
        SkillService.SKILL_NAME + "=执行操作",
        SkillService.SKILL_DESCRIPTION + "=该技能可查询轻舟平台实例、注册应用、应用详情、应用模块详情；支持获取应用模块业务资源列表、资源详情，可读取模块及业务资源实时状态，用于查看资源用量、系统健康检查、安全告警阈值校验。",
        SkillService.SKILL_REQUIRED + "=true"})
public class Skill implements SkillService {
    public static final String ToolLabel = "AppModelActionSkill";

    private final Map<ToolService, Map<String, Object>> aiTools = new ConcurrentHashMap<>();

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE,
            target = "(" + ToolService.PARENT_SKILL + "=" + ToolLabel + ")", // 按服务属性过滤
            unbind = "unbindAiTool")
    public void bindAiTool(ToolService tool, Map<String, Object> properties) {
        aiTools.put(tool, properties);
    }

    // OSGI 框架根据名称规则自动识别调用此方法或在子类的 @Reference 中指定
    public void unbindAiTool(ToolService tool) {
        aiTools.remove(tool);
    }

    @Override
    public Map<ToolService, Map<String, Object>> tools() {
        return aiTools;
    }
}
