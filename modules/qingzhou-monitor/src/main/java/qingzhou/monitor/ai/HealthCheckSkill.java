package qingzhou.monitor.ai;

import org.osgi.service.component.annotations.Component;
import qingzhou.ai.SkillService;

@Component(property = {
        SkillService.SKILL_NAME + "=健康检查",
        SkillService.SKILL_DESCRIPTION + "=当用户意图涉及对平台资源进行性能监控、健康检查时激活此技能。具体激活场景包括但不限于：" +
                "系统巡检、健康检查或状态诊断；查看或汇报当前的资源使用情况；系统指标是否达到安全告警阈值；生成一份结构化的巡检报告。"})
public class HealthCheckSkill implements SkillService {
    @Override
    public String instruction() {
        return "操作指令：\n" +
                "遍历所选的应用，检查应用下的所有模块，检查模块是否具有名字为\"monitor\"的操作，如果有则调用这个操作，该操作返回的数据用作本次系统巡检的素材。\n";
    }
}
