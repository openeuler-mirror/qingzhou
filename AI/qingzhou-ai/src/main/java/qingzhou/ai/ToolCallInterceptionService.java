package qingzhou.ai;

import java.util.Map;

public interface ToolCallInterceptionService {
    String intercept(String toolName, Map<String, Object> args, String principal, String[] roles);
}
