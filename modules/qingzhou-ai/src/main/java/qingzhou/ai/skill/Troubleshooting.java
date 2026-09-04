package qingzhou.ai.skill;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import qingzhou.ai.SkillService;
import qingzhou.ai.ToolService;

@Component(property = SkillService.SKILL_NAME + "=" + SkillService.TROUBLESHOOTING_SKILL)
public class Troubleshooting extends SkillServiceBase implements SkillService {
    public Troubleshooting() {
        super(new String[]{"故障诊断", "en:Troubleshooting"},
                "当用户意图涉及系统异常排查、错误定位、性能瓶颈分析或寻求故障解决方案时激活此技能。具体激活场景包括但不限于：\n" +
                        "- 分析特定的系统日志、错误堆栈（Stack Trace）或告警通知，以定位问题根源；\n" +
                        "- 分析系统运行缓慢、卡顿或资源耗尽的原因，并提供排查思路或优化建议；\n");
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE,
            target = "(" + SkillService.SKILL_NAME + "=" + SkillService.TROUBLESHOOTING_SKILL + ")", // 按服务属性过滤
            unbind = "unbindAiTool")
    public void bindAiTool(ToolService tool, Map<String, Object> properties) {
        aiTools.put(tool, properties);
    }

    // OSGI 框架根据名称规则自动识别调用此方法或在子类的 @Reference 中指定
    public void unbindAiTool(ToolService tool) {
        aiTools.remove(tool);
    }

    @Override
    public Map<AttachmentType, String[]> attachments() {
        return new HashMap<AttachmentType, String[]>() {{
            put(AttachmentType.document, new String[]{".md", ".adoc", ".txt", ".log", ".java"});
            put(AttachmentType.image, new String[]{".jpg", ".jpeg", ".png"});
        }};
    }
}
