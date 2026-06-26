package qingzhou.ai.skill;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import qingzhou.ai.AiSkill;
import qingzhou.ai.AiTool;
import qingzhou.ai.OpenSkills;

@Component(property = AiSkill.SKILL_NAME + "=" + OpenSkills.Troubleshooting)
public class Troubleshooting extends AiSkillBase implements AiSkill {
    public Troubleshooting() {
        super(new String[]{"故障诊断", "en:Troubleshooting"},
                "该技能在用户遇到系统异常、报错、性能瓶颈或明确请求排查问题时触发，旨在通过分析日志、监控数据等定位问题根源并提供解决方案。",
                "技能名称：故障诊断 (Troubleshooting)\n" +
                        "\n" +
                        "触发描述：\n" +
                        "当用户意图涉及系统异常排查、错误定位、性能瓶颈分析或寻求故障解决方案时触发此技能。具体触发场景包括但不限于：\n" +
                        "用户报告了具体的系统异常、报错信息或崩溃现象（如：“服务器报 502 错误”、“数据库连接超时”、“应用频繁重启”）。\n" +
                        "用户要求分析特定的系统日志、错误堆栈（Stack Trace）或告警通知，以定位问题根源。\n" +
                        "用户询问系统运行缓慢、卡顿或资源耗尽的原因，并要求提供排查思路或优化建议。\n" +
                        "用户明确请求进行故障排查、问题诊断或根因分析（如：“帮我诊断一下这个问题”、“排查下网络不通的原因”、“找出宕机的根本原因”）。");
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE,
            target = "(" + AiSkill.SKILL_NAME + "=" + OpenSkills.Troubleshooting + ")", // 按服务属性过滤
            unbind = "unbindAiTool") // 定义在：SkillBase.unbindAiTool
    public void bindAiTool(AiTool tool, Map<String, Object> properties) {
        aiTools.put(tool, properties);
    }

    // OSGI 框架根据名称规则自动识别调用此方法或在子类的 @Reference 中指定
    public void unbindAiTool(AiTool tool) {
        aiTools.remove(tool);
    }
}
