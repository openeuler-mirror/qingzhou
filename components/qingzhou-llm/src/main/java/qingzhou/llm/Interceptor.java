package qingzhou.llm;

import java.util.Map;

public interface Interceptor {
    String interceptTool(String toolName, Map<String, Object> argsMap);
}
