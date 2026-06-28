package qingzhou.registry.ai;

import java.util.*;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import qingzhou.ai.AiTool;
import qingzhou.ai.SystemAiTool;
import qingzhou.api.type.Monitor;
import qingzhou.api.type.Show;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.ResponseImpl;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.AppStub;
import qingzhou.registry.Registry;
import qingzhou.registry.web.WebUtil;

@Component(immediate = true)
public class InvokeActionTool {
    @Reference
    private Registry registry;
    @Reference
    private Logger logger;
    @Reference
    private Json json;

    private BundleContext bundleContext;
    private final List<ServiceRegistration> registrations = new ArrayList<>();

    @Activate
    public void init(ComponentContext context) {
        bundleContext = context.getBundleContext();

        Hashtable<String, String> sharedProperties = new Hashtable<String, String>() {{
            put(AiTool.PARAMETER_NAME + ".1", WebUtil.INSTANCE_ID);
            put(AiTool.PARAMETER_DESCRIPTION + ".1", "应用所在的轻舟实例的 ID，每个应用都有所属的轻舟实例，只有先确定实例，才能确定应用。");

            put(AiTool.PARAMETER_NAME + ".2", WebUtil.APP_CODE);
            put(AiTool.PARAMETER_DESCRIPTION + ".2", "应用的唯一编码，该编码在同一个轻舟实例下不会重复。");

            put(AiTool.PARAMETER_NAME + ".3", WebUtil.MODEL_CODE);
            put(AiTool.PARAMETER_DESCRIPTION + ".3", "模块的唯一编码，该编码在同一个应用内不会重复。");

            put(AiTool.PARAMETER_NAME + ".4", WebUtil.DATA_ID);
            put(AiTool.PARAMETER_DESCRIPTION + ".4", "模块内某条业务数据的唯一ID。如果当前调用的模块同时支持 list 和 show 两种操作，并且用户当前执行的操作不是 list（即为 show 或其他操作），那么此参数为必需参数；否则（即模块不同时具备这两种操作，或当前操作是 list），此参数为非必需参数。");
            put(AiTool.PARAMETER_REQUIRED + ".4", "false");
        }};

        Map<String, String> tools = new HashMap<String, String>() {{
            put(qingzhou.api.type.List.ACTION_CODE_LIST, "该接口返回某个应用模块的业务数据或资源的列表信息。");
            put(Show.ACTION_CODE_SHOW, "该接口返回某模块或模块内某业务数据或资源的详细信息。");
            put(Monitor.ACTION_CODE_MONITOR, "该接口用于获取某模块或模块内某业务数据或资源的实时状态，用来反映资源用量、检查系统健康、告警安全阈值等。");
        }};
        tools.forEach((invokedActionCode, toolDescription) -> {
            Hashtable<String, String> properties = (Hashtable<String, String>) sharedProperties.clone();
            properties.put(AiTool.TOOL_NAME, InvokeActionTool.class.getSimpleName() + "_" + invokedActionCode);
            properties.put(AiTool.TOOL_DESCRIPTION, toolDescription);
            SystemAiTool systemAiTool = toolArgs -> InvokeActionTool.this.invokeActionTool(invokedActionCode, toolArgs);
            registrations.add(bundleContext.registerService(SystemAiTool.class, systemAiTool, properties));
        });
    }

    @Deactivate
    public void destroy() {
        registrations.forEach(ServiceRegistration::unregister);
    }

    private String invokeActionTool(String actionCode, Map<String, Object> toolArgs) {
        if (toolArgs == null) return null;
        String instanceId = (String) toolArgs.get(WebUtil.INSTANCE_ID);
        String appCode = (String) toolArgs.get(WebUtil.APP_CODE);
        String modelCode = (String) toolArgs.get(WebUtil.MODEL_CODE);
        if (instanceId == null || appCode == null || modelCode == null) return null;

        AppStub appStub = registry.getAppStub(instanceId, appCode);
        if (appStub == null) return null;

        RequestImpl request = new RequestImpl();
        request.setInstance(instanceId);
        request.setApp(appCode);
        request.setModel(modelCode);
        request.setAction(actionCode);
        String dataId = (String) toolArgs.get(WebUtil.DATA_ID);
        if (dataId != null) {
            request.setId(dataId);
        }
        try {
            appStub.invokeApp(request);
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }

        ResponseImpl response = request.getResponse();
        try {
            return json.toJson(response);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }
}
