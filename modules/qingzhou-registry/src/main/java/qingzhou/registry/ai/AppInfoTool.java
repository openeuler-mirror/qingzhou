package qingzhou.registry.ai;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.ai.ToolService;
import qingzhou.http.server.HttpHandler;
import qingzhou.json.Json;
import qingzhou.registry.web.AppInfo;
import qingzhou.registry.web.HandlingContext;
import qingzhou.registry.web.WebUtil;

@Component(property = {HttpHandler.HANDLE_PATH + "=/app/info",
        ToolService.PARENT_SKILL + "=" + Skill.TOOL_LABEL,

        ToolService.TOOL_DESCRIPTION + "=该接口返回特定应用的详细信息，内容包括：应用的基本信息（代码标识、名称、描述等等）；应用内包含的业务模块列表信息（模块的代码标识、名称、描述、所属功能菜单等）。",

        ToolService.PARAMETER_NAME + ".1=" + WebUtil.INSTANCE_ID,
        ToolService.PARAMETER_DESCRIPTION + ".1=应用所在的轻舟实例的 ID，每个应用都有所属的轻舟实例，只有先确定实例，才能确定应用。",

        ToolService.PARAMETER_NAME + ".2=" + WebUtil.APP_CODE,
        ToolService.PARAMETER_DESCRIPTION + ".2=应用的唯一编码，该编码在同一个轻舟实例下不会重复。"})
public class AppInfoTool implements ToolService {
    @Reference
    private Json json;
    @Reference
    private AppInfo appInfo;

    @Override
    public String invoke(Map<String, Object> toolArgs) throws Exception {
        if (toolArgs == null) return null;
        HandlingContext context = name -> {
            Object val = toolArgs.get(name);
            return val != null ? String.valueOf(val) : null;
        };
        return json.toJson(appInfo.function.apply(context));
    }
}
