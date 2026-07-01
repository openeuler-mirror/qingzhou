package qingzhou.ai.skill;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import qingzhou.ai.AiSkill;
import qingzhou.ai.AiTool;
import qingzhou.ai.OpenSkills;

@Component(property = AiSkill.SKILL_NAME + "=" + OpenSkills.Inspection)
public class Inspection extends AiSkillBase implements AiSkill {
    public Inspection() {
        super(new String[]{"系统巡检", "en:System Inspection"},
                "当用户意图涉及对服务器、应用或IT基础设施的健康状态检查、性能监控或例行排查时触发此技能。具体触发场景包括但不限于：\n" +
                        "用户明确请求进行系统巡检、健康检查（Health Check）或状态诊断（如：“帮我做个系统巡检”、“检查下服务器状态”）。\n" +
                        "用户要求查看或汇报当前的资源使用情况（如 CPU、内存、磁盘、网络负载等）。\n" +
                        "用户询问系统指标是否达到或超过预设的安全告警阈值。\n" +
                        "用户要求基于当前系统状态生成一份结构化的巡检报告或运维诊断总结。");
    }

    @Override
    public String getInstruction() {
        return "操作指令：\n" +
                "遍历所选的应用，检查应用下的所有模块，检查模块是否具有名字为\"monitor\"的操作，如果有则调用这个操作，该操作返回的数据用作本次系统巡检的素材。\n";
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE,
            target = "(" + AiSkill.SKILL_NAME + "=" + OpenSkills.Inspection + ")", // 按服务属性过滤
            unbind = "unbindAiTool") // 定义在：SkillBase.unbindAiTool
    public void bindAiTool(AiTool tool, Map<String, Object> properties) {
        aiTools.put(tool, properties);
    }

    // OSGI 框架根据名称规则自动识别调用此方法或在子类的 @Reference 中指定
    public void unbindAiTool(AiTool tool) {
        aiTools.remove(tool);
    }
}
