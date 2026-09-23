package qingzhou.registry.ai;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.ai.ToolService;
import qingzhou.json.Json;
import qingzhou.registry.web.Instance;

@Component(property = {
        ToolService.PARENT_SKILL + "=" + Skill.ToolLabel,
        ToolService.TOOL_DESCRIPTION + "=该接口返回轻舟平台上注册的所有轻舟实例的列表信息，每个实例包含实例ID和所在服务器的IP地址等信息。"})
public class InstanceTool implements ToolService {
    @Reference
    private Json json;
    @Reference
    private Instance instance;

    @Override
    public String invoke(Map<String, Object> toolArgs) throws Exception {
        return json.toJson(instance.function.apply(null));
    }
}
