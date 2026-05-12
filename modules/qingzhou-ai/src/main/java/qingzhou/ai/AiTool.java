package qingzhou.ai;

import java.util.Map;

public interface AiTool {
    String TOOL_DESCRIPTION = "TOOL_DESCRIPTION";

    default ToolParameter[] parameters() {
        return null;
    }

    Object invoke(Map<String, Object> argsMap);
}
