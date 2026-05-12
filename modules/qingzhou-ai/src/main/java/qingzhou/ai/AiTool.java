package qingzhou.ai;

import java.util.Map;

import qingzhou.llm.Parameter;

public interface AiTool {
    String TOOL_DESCRIPTION = "TOOL_DESCRIPTION";

    default Parameter[] parameters() {
        return null;
    }

    Object invoke(Map<String, Object> argsMap);
}
