package qingzhou.ai;

import java.util.Map;

public interface AiTool {
    String TOOL_SKILL_NAME = "TOOL_SKILL_NAME"; // 所属技能

    String TOOL_NAME = "TOOL_NAME";
    String TOOL_DESCRIPTION = "TOOL_DESCRIPTION";

    String PARAMETER_NAME = "PARAMETER_NAME";
    String PARAMETER_DESCRIPTION = "PARAMETER_DESCRIPTION";
    String PARAMETER_REQUIRED = "PARAMETER_REQUIRED";

    String invoke(Map<String, Object> toolArgs) throws Exception;
}
