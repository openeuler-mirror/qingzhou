package qingzhou.registry.ai;

import java.util.*;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import qingzhou.ai.ToolInterceptor;
import qingzhou.ai.ToolService;
import qingzhou.api.action.Monitor;
import qingzhou.api.action.Page;
import qingzhou.api.action.Show;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.ResponseImpl;
import qingzhou.dto.meta.annotation.App;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.AppStub;
import qingzhou.registry.Registry;
import qingzhou.registry.web.PermissionChecker;
import qingzhou.registry.web.WebUtil;

@Component(immediate = true)
public class RegisterActionTools {
    private static final String PAGE_NUM = "pageNum";

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
            put(ToolService.PARAMETER_NAME + ".1", WebUtil.INSTANCE_ID);
            put(ToolService.PARAMETER_DESCRIPTION + ".1", "应用所在的轻舟实例的 ID，每个应用都有所属的轻舟实例，只有先确定实例，才能确定应用。");

            put(ToolService.PARAMETER_NAME + ".2", WebUtil.APP_CODE);
            put(ToolService.PARAMETER_DESCRIPTION + ".2", "应用的唯一编码，该编码在同一个轻舟实例下不会重复。");

            put(ToolService.PARAMETER_NAME + ".3", WebUtil.MODEL_CODE);
            put(ToolService.PARAMETER_DESCRIPTION + ".3", "模块的唯一编码，该编码在同一个应用内不会重复。");

            put(ToolService.PARAMETER_NAME + ".4", WebUtil.DATA_ID);
            put(ToolService.PARAMETER_DESCRIPTION + ".4", "模块内某条业务数据的唯一ID。如果当前调用的模块同时支持 list 和 show 两种操作，并且用户当前调用的操作不是 list（即为 show 或其他操作），那么此参数为必需参数；否则（即模块不同时具备这两种操作，或当前操作是 list），此参数为非必需参数。");
            put(ToolService.PARAMETER_REQUIRED + ".4", "false");
        }};

        Map<String, String> tools = new HashMap<String, String>() {{
            put(Page.ACTION_CODE_PAGE, "该接口返回某个应用模块的业务数据或资源的列表信息。列表数据分页返回，每次调用只返回一页，可通过 " + PAGE_NUM + " 指定页码。"
                    + "如果返回内容以【分页提示】开头，说明列表共有多页，此时须遵守以下规则："
                    + "1. 即使用户要求列出全部数据，也只查询这一页；用户指定页码前，不要查询其他页，也不要通过猜测编号、逐条调用其他工具等方式获取本页以外的数据。"
                    + "2. 如实列出本页数据，禁止推测或编造其他页的内容；如果用户的问题涉及全部数据（如统计、排序、查找最大值），须说明结论仅基于本页。"
                    + "3. 如果返回内容被截断，如实说明本页有部分数据未能显示，不要推测被截断的内容，也不要为补全而重复调用工具。"
                    + "4. 回复末尾注明“第 X/Y 页”（最后一页注明“已是最后一页”），并提示用户可回复页码（如“第 2 页”或“下一页”）查看其他页；页码超出范围时，告知用户有效的页码范围。"
                    + "5. 用户指定页码后，使用相同的参数并传入 " + PAGE_NUM + " 再次调用本工具。");
            put(Show.ACTION_CODE_SHOW, "该接口返回某模块或模块内某业务数据或资源的详细信息。");
            put(Monitor.ACTION_CODE_MONITOR, "该接口用于获取某模块或模块内某业务数据或资源的实时状态，用来反映资源用量、检查系统健康、告警安全阈值等。");
        }};
        tools.forEach((invokedActionCode, toolDescription) -> {
            String actionToolName = "app_model_action_" + invokedActionCode;
            Hashtable<String, String> properties = (Hashtable<String, String>) sharedProperties.clone();
            properties.put(ToolService.PARENT_SKILL, InvokeActionSkill.TOOL_LABEL);
            properties.put(ToolService.TOOL_NAME, actionToolName);
            if (Page.ACTION_CODE_PAGE.equals(invokedActionCode)) {
                properties.put(ToolService.PARAMETER_NAME + ".5", PAGE_NUM);
                properties.put(ToolService.PARAMETER_DESCRIPTION + ".5", "列表页码，从 1 开始，不传则为第 1 页。仅在用户指定要查看某一页时传入。");
                properties.put(ToolService.PARAMETER_REQUIRED + ".5", "false");
            }
            properties.put(ToolService.TOOL_DESCRIPTION, toolDescription);
            ToolService systemToolService = toolArgs -> RegisterActionTools.this.invokeActionTool(invokedActionCode, toolArgs);
            registrations.add(bundleContext.registerService(ToolService.class, systemToolService, properties));
            registrations.add(bundleContext.registerService(ToolInterceptor.class, (toolName, toolArgs, c) -> {
                if (toolName.equals(actionToolName)) {
                    String[] roles = c.getRoles();
                    AppStub appStub = retrieveAppStub(toolArgs);
                    String modelCode = (String) toolArgs.get(WebUtil.MODEL_CODE);
                    if (appStub == null || modelCode == null) return null;

                    boolean allowed = PermissionChecker.isAllowed(appStub.getAppMeta().getApp(), modelCode, invokedActionCode, roles);
                    if (allowed) return null;
                    else return "操作执行失败：当前身份缺少对应工具执行权限，请检查权限配置后重试。";
                }
                return null;// 不拦截
            }, null));
        });
    }

    @Deactivate
    public void destroy() {
        registrations.forEach(ServiceRegistration::unregister);
    }

    private AppStub retrieveAppStub(Map<String, Object> toolArgs) {
        if (toolArgs == null) return null;
        String instanceId = (String) toolArgs.get(WebUtil.INSTANCE_ID);
        String appCode = (String) toolArgs.get(WebUtil.APP_CODE);
        String modelCode = (String) toolArgs.get(WebUtil.MODEL_CODE);
        if (instanceId == null || appCode == null || modelCode == null) return null;

        return registry.getAppStub(instanceId, appCode);
    }

    private String invokeActionTool(String actionCode, Map<String, Object> toolArgs) {
        AppStub appStub = retrieveAppStub(toolArgs);
        if (appStub == null) return null;

        App app = appStub.getAppMeta().getApp();
        String instanceId = (String) toolArgs.get(WebUtil.INSTANCE_ID);
        String modelCode = (String) toolArgs.get(WebUtil.MODEL_CODE);

        RequestImpl request = new RequestImpl();
        request.setInstance(instanceId);
        request.setApp(app.code);
        request.setModel(modelCode);
        request.setAction(actionCode);
        String dataId = (String) toolArgs.get(WebUtil.DATA_ID);
        if (dataId != null) {
            request.setId(dataId);
        }
        applyPageNum(request, toolArgs.get(PAGE_NUM));
        try {
            appStub.invokeApp(request);
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }

        ResponseImpl response = request.getResponse();
        String pagingHint = Page.ACTION_CODE_PAGE.equals(actionCode) ? buildPagingHint(response) : null;
        try {
            String result = json.toJson(response);
            // 工具返回内容发给大模型前会按长度截断（见 qingzhou-llm 的 maxToolResultChars），分页提示须放在最前面才不会被截掉
            return pagingHint == null ? result : pagingHint + "\n" + result;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    private static void applyPageNum(RequestImpl request, Object pageNum) {
        if (pageNum == null) return;

        String value = pageNum instanceof Number ? String.valueOf(((Number) pageNum).intValue()) : pageNum.toString().trim();
        request.getParameters().put(PAGE_NUM, value);
    }

    private static String buildPagingHint(ResponseImpl response) {
        if (!response.isSuccess() || !(response.getData() instanceof Map)) return null;

        Map<?, ?> data = (Map<?, ?>) response.getData();
        Object pageNum = data.get(PAGE_NUM);
        Object pageSize = data.get("pageSize");
        Object totalSize = data.get("totalSize");
        if (!(pageNum instanceof Number && pageSize instanceof Number && totalSize instanceof Number)) return null;

        return pagingHint(((Number) pageNum).intValue(), ((Number) pageSize).intValue(), ((Number) totalSize).intValue());
    }

    // 提示须尽量简短：它排在返回内容最前面，每多一个字符，末尾就少显示一个字符的数据；固定的翻页规则写在工具描述里
    private static String pagingHint(int pageNum, int pageSize, int totalSize) {
        if (pageSize <= 0 || totalSize <= 0) return null;

        int totalPages = (totalSize + pageSize - 1) / pageSize;
        if (pageNum > totalPages) {
            return String.format("【分页提示】请求的第 %d 页超出范围，有效页码为 1～%d（共 %d 条），本页无数据。", pageNum, totalPages, totalSize);
        }
        if (totalPages == 1) return null;

        String ending = pageNum == totalPages ? "已是最后一页。" : "用户指定页码前不要查询其他页。";
        return String.format("【分页提示】本次为第 %d 页，共 %d 页（每页 %d 条，共 %d 条），%s", pageNum, totalPages, pageSize, totalSize, ending);
    }
}
