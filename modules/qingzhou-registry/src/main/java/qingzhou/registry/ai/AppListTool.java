package qingzhou.registry.ai;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.ai.ToolService;
import qingzhou.json.Json;
import qingzhou.registry.web.AppList;
import qingzhou.registry.web.RequestParameters;

@Component(property = {
        ToolService.PARENT_SKILL + "=" + InvokeActionSkill.TOOL_LABEL,
        ToolService.TOOL_DESCRIPTION + "=该接口返回已注册的应用列表信息。每个应用包含唯一标识、名称、描述、所属实例等信息。"})
public class AppListTool implements ToolService {
    @Reference
    private Json json;
    @Reference
    private AppList appList;

    @Override
    public String invoke(Map<String, Object> toolArgs) throws Exception {
        return json.toJson(appList.getFunction().apply(RequestParameters.of(toolArgs)));
    }
}
