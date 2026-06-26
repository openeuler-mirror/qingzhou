package qingzhou.registry.service;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.ai.AiTool;
import qingzhou.ai.SystemAiTool;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelAction;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.AppStub;
import qingzhou.registry.Registry;
import qingzhou.registry.impl.WebUtil;

@Component(property = {
        AiTool.TOOL_DESCRIPTION + "=该接口用于检查某个模块是否具有某个操作，返回true表示有，false则没有，其它则输入参数有误。如果只需要知道某模块是否具有某操作或方法，那么应该调用此方法而不是调用获取应用模块详细信息的方法。",

        AiTool.PARAMETER_NAME + ".1=" + WebUtil.INSTANCE_ID,
        AiTool.PARAMETER_DESCRIPTION + ".1=应用所在的轻舟实例 ID，用于区分不同实例上的相同应用",

        AiTool.PARAMETER_NAME + ".2=" + WebUtil.APP_CODE,
        AiTool.PARAMETER_DESCRIPTION + ".2=应用唯一编码，该编码在同一个轻舟实例下不会重复",

        AiTool.PARAMETER_NAME + ".3=" + WebUtil.MODEL_CODE,
        AiTool.PARAMETER_DESCRIPTION + ".3=模块唯一编码，该编码在同一个应用内不会重复",

        AiTool.PARAMETER_NAME + ".4=" + WebUtil.ACTION_CODE,
        AiTool.PARAMETER_DESCRIPTION + ".4=待检查的操作名"
})
public class IfActionExistsTool implements SystemAiTool {
    @Reference
    private Registry registry;
    @Reference
    private Logger logger;
    @Reference
    private Json json;

    @Override
    public String invoke(Map<String, Object> toolArgs) {
        if (toolArgs == null) return null;
        String instanceId = (String) toolArgs.get(WebUtil.INSTANCE_ID);
        String appCode = (String) toolArgs.get(WebUtil.APP_CODE);
        String modelCode = (String) toolArgs.get(WebUtil.MODEL_CODE);
        String actionCode = (String) toolArgs.get(WebUtil.ACTION_CODE);
        if (instanceId == null || appCode == null || modelCode == null || actionCode == null) return null;

        AppStub appStub = registry.getAppStub(instanceId, appCode);
        if (appStub == null) return null;
        for (Model model : appStub.getAppMeta().getApp().models) {
            for (ModelAction action : model.actions) {
                if (action.code.equals(actionCode)) return "true";
            }
        }
        return "false";
    }
}
