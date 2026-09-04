package qingzhou.ai.skill;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import qingzhou.ai.SkillService;
import qingzhou.ai.ToolService;

@Component(property = SkillService.SKILL_NAME + "=" + SkillService.HEALTH_CHECK_SKILL)
public class HealthCheck extends SkillServiceBase implements SkillService {
    public HealthCheck() {
        super(new String[]{"系统巡检", "en:System Inspection"},
                "当用户意图涉及对平台资源进行性能监控、健康检查时激活此技能。具体激活场景包括但不限于：\n" +
                        "- 系统巡检、健康检查（Health Check）或状态诊断（如：“帮我做个系统巡检”、“检查下服务器状态”）；\n" +
                        "- 查看或汇报当前的资源使用情况（如 CPU、内存、磁盘、网络负载等）；\n" +
                        "- 系统指标是否达到安全告警阈值；\n" +
                        "- 生成一份结构化的巡检报告；\n");
    }

    @Override
    public String instruction() {
        return "操作指令：\n" +
                "遍历所选的应用，检查应用下的所有模块，检查模块是否具有名字为\"monitor\"的操作，如果有则调用这个操作，该操作返回的数据用作本次系统巡检的素材。\n";
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE,
            target = "(" + SkillService.SKILL_NAME + "=" + SkillService.HEALTH_CHECK_SKILL + ")", // 按服务属性过滤
            unbind = "unbindAiTool")
    public void bindAiTool(ToolService tool, Map<String, Object> properties) {
        aiTools.put(tool, properties);
    }

    // OSGI 框架根据名称规则自动识别调用此方法或在子类的 @Reference 中指定
    public void unbindAiTool(ToolService tool) {
        aiTools.remove(tool);
    }
}
