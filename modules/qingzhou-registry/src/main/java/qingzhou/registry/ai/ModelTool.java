package qingzhou.registry.ai;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.ai.ToolService;
import qingzhou.json.Json;
import qingzhou.registry.web.AppModel;
import qingzhou.registry.web.HandlingContext;
import qingzhou.registry.web.WebUtil;

@Component(property = {
        ToolService.PARENT_SKILL + "=" + Skill.ToolLabel,

        ToolService.TOOL_DESCRIPTION + "=该接口返回特定应用的特定模块的详细信息，内容包括：模块的概要信息和模块内定义的数据字段列表和支持的操作列表。对于数据字段，可提供每个字段的数据类型、显示行为、取值校验等信息，每个操作包含代码、图标、顺序、操作名称、描述，以及该操作在列表头、列表行或批处理场景下的可用性标识。通过该接口可理解一个功能模块有哪些可用的数据字段、每个字段的填写和展示规则，以及该模块支持哪些操作，从而动态生成数据管理界面或执行相应的数据操作。",

        ToolService.PARAMETER_NAME + ".1=" + WebUtil.INSTANCE_ID,
        ToolService.PARAMETER_DESCRIPTION + ".1=应用所在的轻舟实例的 ID，每个应用都有所属的轻舟实例，只有先确定实例，才能确定应用。",

        ToolService.PARAMETER_NAME + ".2=" + WebUtil.APP_CODE,
        ToolService.PARAMETER_DESCRIPTION + ".2=应用的唯一编码，该编码在同一个轻舟实例下不会重复。",

        ToolService.PARAMETER_NAME + ".3=" + WebUtil.MODEL_CODE,
        ToolService.PARAMETER_DESCRIPTION + ".3=模块的唯一编码，该编码在同一个应用内不会重复。"})
public class ModelTool implements ToolService {
    @Reference
    private Json json;
    @Reference
    private AppModel appModel;

    @Override
    public String invoke(Map<String, Object> toolArgs) throws Exception {
        HandlingContext context = name -> {
            if (toolArgs != null) {
                Object val = toolArgs.get(name);
                return val != null ? String.valueOf(val) : null;
            }
            return null;
        };
        return json.toJson(appModel.function.apply(context));
    }
}
