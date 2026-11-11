package qingzhou.registry.ai;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.ai.ToolService;
import qingzhou.dto.meta.Model;
import qingzhou.dto.meta.ModelAction;
import qingzhou.registry.AppStub;
import qingzhou.registry.Registry;
import qingzhou.registry.web.WebParameters;

@Component(property = {
        ToolService.TOOL_LABEL + "=" + InvokeActionSkill.TOOL_LABEL,

        ToolService.TOOL_NAME + "=ActionCheckTool",
        ToolService.TOOL_DESCRIPTION + "=该接口用于检查某个模块是否具有某个操作，返回true表示有，false则没有，其它则输入参数有误。如果只需要知道某模块是否具有某操作或方法，那么应该调用此方法而不是调用获取应用模块详细信息的方法。",

        ToolService.PARAMETER_NAME + ".1=" + WebParameters.INSTANCE_ID,
        ToolService.PARAMETER_DESCRIPTION + ".1=应用所在的轻舟实例 ID，用于区分不同实例上的相同应用",

        ToolService.PARAMETER_NAME + ".2=" + WebParameters.APP_CODE,
        ToolService.PARAMETER_DESCRIPTION + ".2=应用唯一编码，该编码在同一个轻舟实例下不会重复",

        ToolService.PARAMETER_NAME + ".3=" + WebParameters.MODEL_CODE,
        ToolService.PARAMETER_DESCRIPTION + ".3=模块唯一编码，该编码在同一个应用内不会重复",

        ToolService.PARAMETER_NAME + ".4=" + WebParameters.ACTION_CODE,
        ToolService.PARAMETER_DESCRIPTION + ".4=待检查的操作名"})
public class ActionCheckTool implements ToolService {
    @Reference
    private Registry registry;

    @Override
    public String invoke(Map<String, Object> toolArgs) {
        if (toolArgs == null) return null;
        String instanceId = (String) toolArgs.get(WebParameters.INSTANCE_ID);
        String appCode = (String) toolArgs.get(WebParameters.APP_CODE);
        String modelCode = (String) toolArgs.get(WebParameters.MODEL_CODE);
        String actionCode = (String) toolArgs.get(WebParameters.ACTION_CODE);
        if (instanceId == null || appCode == null || modelCode == null || actionCode == null) return null;

        AppStub appStub = registry.getAppStub(instanceId, appCode);
        if (appStub == null) return null;
        for (Model model : appStub.getAppMeta().getApp().models) {
            if (model.code.equals(modelCode)) {
                for (ModelAction action : model.actions) {
                    if (action.code.equals(actionCode)) return "true";
                }
                break;
            }
        }
        return "false";
    }
}
