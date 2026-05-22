package qingzhou.ai;

import java.util.Map;

public interface AiTool {
    String TOOL_NAME = "TOOL_NAME";
    String TOOL_DESCRIPTION = "TOOL_DESCRIPTION";

    String PARAMETER_NAME = "PARAMETER_NAME";
    String PARAMETER_DESCRIPTION = "PARAMETER_DESCRIPTION";
    String PARAMETER_REQUIRED = "PARAMETER_REQUIRED";

    Object invoke(Map<String, Object> toolArgs);
}
