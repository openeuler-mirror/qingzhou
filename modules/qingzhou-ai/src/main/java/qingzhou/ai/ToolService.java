package qingzhou.ai;

import java.util.Map;

public interface ToolService {
    String TOOL_NAME = "TOOL_NAME";
    String TOOL_DESCRIPTION = "TOOL_DESCRIPTION";

    String PARAMETER_NAME = "PARAMETER_NAME";
    String PARAMETER_DESCRIPTION = "PARAMETER_DESCRIPTION";
    String PARAMETER_REQUIRED = "PARAMETER_REQUIRED";

    String invoke(Map<String, Object> toolArgs) throws Exception;

    /**
     * 携带调用者角色的调用入口：默认忽略角色，实现方可覆写以执行权限判定。
     * 角色必须由服务端鉴权结果提供，不得取自工具参数。
     */
    default String invoke(Map<String, Object> toolArgs, String[] roles) throws Exception {
        return invoke(toolArgs);
    }
}
