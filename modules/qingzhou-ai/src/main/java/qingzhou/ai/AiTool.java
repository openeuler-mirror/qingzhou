package qingzhou.ai;

import java.util.Map;

public interface AiTool {
    String TOOL_DESCRIPTION = "TOOL_DESCRIPTION";

    String PARAMETER_NAME = "PARAMETER_NAME";
    String PARAMETER_DESCRIPTION = "PARAMETER_DESCRIPTION";

    Object invoke(Map<String, Object> toolArgs);
}
