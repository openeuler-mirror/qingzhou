package qingzhou.ai.skill.health.check;

import org.osgi.service.component.annotations.Component;
import qingzhou.ai.SkillService;

/**
 * 巡检技能：只注入提示词、不挂工具，也未声明 SKILL_REQUIRED，故仅在 AI Chat 按需激活、不进入 MCP 工具集。
 * 动作调用统一由「执行操作」技能提供，本技能只描述巡检数据的取值范围，避免重复遍历。
 */
@Component(property = {
        SkillService.SKILL_NAME + "=健康检查",
        SkillService.SKILL_DESCRIPTION + "=当用户意图涉及对平台资源进行性能监控、健康检查时激活此技能。具体激活场景包括但不限于：" +
                "系统巡检、健康检查或状态诊断；查看或汇报当前的资源使用情况；系统指标是否达到安全告警阈值；生成一份结构化的巡检报告。"})
public class HealthCheckSkill implements SkillService {
    @Override
    public String instruction() {
        return "操作指令：\n" +
                "遍历所选应用下具有 监视 monitor 操作的模块并调用该操作，返回数据作为本次巡检的素材；\n" +
                "该操作返回的是模块状态快照，数值指标与阈值判定以字段声明的 numeric 标记为准，文本字段不要当作度量值。";
    }
}
