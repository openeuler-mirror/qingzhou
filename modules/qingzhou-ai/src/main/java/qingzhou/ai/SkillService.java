package qingzhou.ai;

import java.util.Map;

public interface SkillService {
    String HEALTH_CHECK = "HealthCheck";
    String SKILL_TROUBLESHOOTING = "Troubleshooting";
    String SKILL_PLATFORM_HELP = "PlatformHelp";

    String NAME = "SKILL_NAME";

    String[] displayNames();

    String description();

    // 技能的说明书：激活后注入系统提示词，可用于引导 AI 如何使用该技能下的工具，如果没有工具，那就只是一段提示词增强
    default String instruction() {
        return null;
    }

    // 技能的工具集：该技能需要挂载的功能工具
    Map<ToolService, Map<String, Object>> tools();

    // 返回附件类型和支持的后缀，客户端会回传此类型
    default Map<AttachmentType, String[]> supportedAttachmentTypes() {
        return null;
    }

    enum AttachmentType {
        text, image
    }
}
